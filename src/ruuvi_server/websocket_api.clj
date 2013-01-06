(ns ruuvi-server.websocket-api
   (:use
    [lamina.core :only (enqueue named-channel sample-every receive-all siphon close on-closed map*)]
    [aleph.http :only (wrap-aleph-handler start-http-server wrap-ring-handler)]
    [aleph.formats :only (encode-json->string decode-json)]

    compojure.core
    compojure.response
    ring.middleware.reload
    ring.middleware.file
    ring.adapter.jetty
    [ring.util.response :only (response content-type)]
   )
   (:require
     [compojure.handler :as handler]
   )
)

(def ^{:private true} clients (atom 0))
(def ^{:private true} received-messages (atom 0))

;; stuff sent to this, is send to every client
(def ^{:private true} broadcast-channel (named-channel :broadcast nil))

;; internal channel
(def ^{:private true} world-channel (named-channel :world nil))

(def ^{:private true} trackers (atom {}))

(defn- enqueue-json
  "Converts data to JSON and sends it to channel"
  [ch data]
  (enqueue ch (encode-json->string data)))  

(defn subscribe
  "Subscribe channel to tracker. Channel will receive traffic events from the tracker."
  [tracker-id ch]
  (swap! trackers (fn [old]
                    (update-in old [tracker-id]
                               (fn [values]
                                 (into #{} (conj values ch))
                                 ))))
  )

(defn unsubscribe
  "Unsubscribe channel from tracker. Channel will stop receiving events from the tracker."
  [tracker-id ch]
  (swap! trackers (fn [old]
                    (update-in old [tracker-id]
                               (fn [values]
                                 (disj values ch)
                                 ))))
  )

(defn unsubscribe-all
  "Unsubscribe channel from all trackers. Channel will stop receiving events from all trackers."
  [ch]
  (swap! trackers
         (fn [old]
           (into {}
                 (map (fn [[tracker-id channels]]
                        {tracker-id (disj channels ch)}
                        ) old))))
  )
   
(defn- start-messaging [ch]
  (enqueue world-channel {:t 0 :clients 0})
  (let [clock (sample-every 5000 world-channel)]
    (receive-all clock
                 (fn [m] (enqueue world-channel {:t (+ (:t m) 1)
                                                 :clients @clients
                                                 :msgs @received-messages}) )
                 )
    (siphon world-channel ch)
    )
  )


(defn publish-event
  "Publish event from tracker. All subscribed clients will receive the event."
  [tracker-id event]
  (let [channels (@trackers tracker-id)]
    (dorun
     (map (fn [ch] 
            (enqueue-json ch event)
            ) channels)
     )))


(defn- process-ping
  "Process ping message. Replies with pong message."
  [ch data]
  (let [value (:ping data)]
    (enqueue-json ch {:pong value})
    )
  )

(defn- process-subscribe
  "Process subscribe message."
  [ch data]
  (dorun (map (fn [tracker-id]
                (subscribe tracker-id ch)
                ) (:ids data)))
  )

(defn- process-unsubscribe
  "Process unsubscribe message"
  [ch data]
  (dorun (map (fn [tracker-id]
                (unsubscribe tracker-id ch)
                ) (:ids data)))
  )

(defn- process-new-event
  "Process new-event message. Publishes event."
  [ch data]
  ;; TODO store event to db
  ;; call publish-event
  )

(defn- process-unknown-command
  "Process unknown command. Sends error message and closes channel."
  [ch data]
  (enqueue-json ch {:error "unknown command"})
  (close ch) )

(defn- handle-client-message [ch incoming-data]
  (swap! received-messages inc)
  (try
    (let [data (decode-json incoming-data)]
      (cond (:ping data) (process-ping ch data)
            (:subscribe data) (process-subscribe ch data)
            (:unsubscribe data) (process-unsubscribe ch data)
            (:new-event data) (process-new-event ch data)
            :else (process-unknown-command ch data)
            )
      )
    (catch Exception e
      ;; TODO this may fail, if channel closes at right time
      (enqueue ch (encode-json->string {:error "Not JSON data. Closing connection.",
                                        :exception (str e)}))
      (close ch)
      )
    ))

(defn- close-client [ch]
  (unsubscribe-all ch)
  (swap! clients dec))

(defn- websocket-handler [ch request]
  (on-closed ch (fn [] (close-client ch)))
  (swap! clients inc)
  ;; handle-client message handles all messages from client
  (map* (fn [data] (handle-client-message ch data)) ch)
  ;; send broadcast-channel data to client
  (siphon (map* encode-json->string broadcast-channel) ch)
)

(defn websocket-api-handler
  "Ring handled that can be directly plugged into Compojure routes"
  []
  (wrap-aleph-handler websocket-handler))

(defn websocket-api-init
  "Init function for websocket-api."
  []
  (start-messaging broadcast-channel))

;; testcode
(defroutes routez
  (GET "/" [] "some data")
  (GET "/ws" [] (websocket-api-handler))
)
 
(start-http-server (wrap-ring-handler (handler/site routez))
                   {:port 8081 :websocket true})
 
(websocket-api-init)
