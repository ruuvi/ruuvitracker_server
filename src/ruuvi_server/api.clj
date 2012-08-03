(ns ruuvi-server.api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.tracker-api :as tracker-api])
  (:require [ruuvi-server.client-api :as client-api])
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:use ring.middleware.json-params)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(def request-counter (atom 0))

(defn wrap-request-logger
  "Logs each incoming request"
  [app]
  (fn [request]
    (let [counter (swap! request-counter inc)
          request-method (:request-method request)
          uri (:uri request)
          query-params (:query-params request)
          start (System/currentTimeMillis)
          remote-addr (:remote-addr request)]
      (info (str "REQUEST:" counter)
            remote-addr request-method uri ":query-params" query-params)
      (let [response (app request)
            duration (- (System/currentTimeMillis) start)
            status (:status response)]
        (info (str "RESPONSE:" counter)
              remote-addr
              status
              duration "msec")
        response)
    )))


(def url-prefix "/v1-dev")

(defn- success-handler[request]
  {:status 200
   :headers {"Content-Type" "application/json;charset=UTF-8"}})

(def id-list-regex #"([0-9]+,?)+")

;; TODO do some macro thing that creates automatically OPTIONS route
(defroutes api-routes-internal

  ;; Client-API
  (OPTIONS "/ping" []
           (-> #'success-handler))
  (GET "/ping" []
       (-> #'client-api/ping))
  
  (OPTIONS ["/trackers/:ids" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/trackers/:ids" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           ))
  
  (OPTIONS ["/trackers/:ids/events" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/trackers/:ids/events" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-events (merge request {:tracker_ids ids})))
           ))

  ;; TODO missing regexp for id list and order
  (OPTIONS "/trackers/:ids/events/:order" [ids latest]
           (-> #'success-handler))
  (GET ["/trackers/:ids/events/:order" :order #"latest|latestStored"] [ids order]
       (-> (fn [request]
             (let [order-by (cond (= order "latest") :latest-event-time
                                  (= order "latestStored") :latest-store-time)]
               (client-api/fetch-events (merge request {:tracker_ids ids :order-by order-by}))))
           ))
  
  (OPTIONS ["/trackers/:ids/sessions" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/trackers/:ids/sessions" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-session (merge request {:tracker_ids ids})))
           ))

  (OPTIONS ["/sessions/:ids/events" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids/events" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-events
                          (merge request {:event_session_ids ids})))
           ))

  (OPTIONS ["/sessions/:ids/events/latest" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids/events/latest" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-events
                          (merge request {:event_session_ids ids})))
           ))

  
  (OPTIONS ["/sessions/:ids" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-session (merge request {:event_session_ids ids})))
           ))

  (OPTIONS ["/sessions/:ids/events" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids/events" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           ))
  
  (OPTIONS "/trackers" []
           (-> #'success-handler))
  (GET "/trackers" []
       (-> #'client-api/fetch-trackers ))
  
  (OPTIONS ["/events/:ids" :ids #"([0-9+],?)+"] [ids]
           (-> #'success-handler))
  (GET ["/events/:ids" :ids #"([0-9+],?)+"] [ids]
       (-> (fn [request] (client-api/fetch-event request ids))
           ))

  (OPTIONS "/events" []
           (-> #'success-handler))
  (GET "/events" []
       (-> #'client-api/fetch-events))
  ;; Tracker-API
  (POST "/events" []
        (-> #'tracker-api/handle-create-event))

  )

(defroutes api-routes
  (context url-prefix []
           (-> api-routes-internal
               (util/wrap-cors-headers)
               (wrap-request-logger) ))
  (route/not-found {:status 200 :body "foo" :content-type "application/json"})
  )