(ns ruuvi-server.api
  (:use ruuvi-server.common)
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:require [clj-json.core :as json])
  (:use ring.middleware.json-params)
  (:use ring.middleware.params)
  (:use ring.middleware.session)
  (:use ring.middleware.cookies)
  )

(def logger (org.slf4j.LoggerFactory/getLogger "ruuvi-server.api"))

(defn timestamp [] (.toString (new org.joda.time.DateTime)))

(defn shared-secret [trackerid]  "VerySecret1"  )

(defn json-response
  "Formats data map as JSON" 
  [request data & [status]]
  (let [jsonp-function ((request :params) :jsonp)
        body (if jsonp-function
              (str jsonp-function "(" (json/generate-string data) ")")
              (json/generate-string data))]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body body}))

(defn ping [request]
  (json-response request {"ruuvi-tracker-protocol-version" "1"
                  "server-software" (str server-name "/" server-version)
                  "time" (timestamp)}))

(defn create-event [request]
  (if (request :authenticated-tracker) 
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "accepted"}
    {:status 401
     :headers {"Content-Type" "text/plain"}
     :body "not authorized"}
   ))

(defn fetch-trackers [request]
  "not implemented yet")

(defn fetch-events [request]
  "not implemented yet")

(defn compute-mac [params]
  (let [secret (shared-secret (params :trackerid))
        request-mac (params :mac)
        ; sort keys alphabetically
        sorted-keys (sort (keys params))
        ; remove :mac key
        included-keys (filter (fn [param-key]
                                (not= :mac param-key))
                              sorted-keys)
        ; make included-keys a vector and convert to non-lazy list
        param-keys (vec included-keys)]
    
   ; concatenate keys, values and separators. also add shared secret
   (let [value (str (apply str (for [k param-keys]
                                  (str (name k) ":" (params k) "|")
                                  )))
         value-with-shared-secret (str value secret)
         messageDigester (java.security.MessageDigest/getInstance "SHA-1")]
      (let [computed-mac (.digest messageDigester (.getBytes value-with-shared-secret "ASCII"))
            computed-mac-hex (org.apache.commons.codec.binary.Hex/encodeHexString computed-mac)]
        (.debug logger (str "Value: " value-with-shared-secret))   
        (.debug logger (str  "orig-mac "(str request-mac) " computed mac " (str computed-mac-hex)) )
        computed-mac-hex
        ))))

(defn wrap-create-event-auth
  "Sets key :create-event-auth"
  [app]
  (fn [request]
    (let [params (request :params)]
      (if (not (params :mac))
        (app request)
        (let [computed-mac (compute-mac params)
              request-mac (params :mac)]
          (if (= computed-mac request-mac)
            (app (merge request {:authenticated-tracker true}))
            (app request)
            ))))))

(defn wrap-request-logger
  "Logs each incoming request"
  [app request-name]
  (fn [request]
    (.info logger (str "Received " request-name "-request from " (request :remote-addr) ))
    (app request)
    ))

(defroutes api-routes
  (GET "/ping" [] (-> #'ping
                      (wrap-request-logger "ping")
                      ))
  (GET "/events" [] (-> #'create-event
                        (wrap-request-logger "create-event")
                        (wrap-create-event-auth)
                        ))
  (GET "/trackers" [] (-> #'fetch-trackers
                          (wrap-request-logger "fetch-trackers")
                          (wrap-json-params)
                          ))
  (GET "/events" [] (-> #'fetch-events
                        (wrap-request-logger "fetch-events")
                        (wrap-json-params)
                        )))
