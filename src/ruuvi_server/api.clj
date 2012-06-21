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


;; TODO do some macro thing that creates automatically OPTIONS route
(defroutes api-routes
  ;; Client-API
  (OPTIONS (str url-prefix "/ping") []
       (-> #'success-handler
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET (str url-prefix "/ping") []
       (-> #'client-api/ping
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (OPTIONS [(str url-prefix "/trackers/:ids") :ids #"([0-9]+,?)+"] [ids]
       (-> #'success-handler
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET [(str url-prefix "/trackers/:ids") :ids #"([0-9]+,?)+"] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (OPTIONS [(str url-prefix "/trackers/:ids/events") :ids #"([0-9]+,?)+"] [ids]
       (-> #'success-handler
           (util/wrap-cors-headers "GET" )
           (wrap-request-logger)))
  (GET [(str url-prefix "/trackers/:ids/events") :ids #"([0-9]+,?)+"] [ids]
       (-> (fn [request] (client-api/fetch-events (merge request {:tracker_ids ids})))
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (OPTIONS (str url-prefix "/trackers") []
       (-> #'success-handler
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET (str url-prefix "/trackers") []
       (-> #'client-api/fetch-trackers
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))

  (OPTIONS [(str url-prefix "/events/:ids") :ids #"([0-9+],?)+"] [ids]
       (-> #'success-handler
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET [(str url-prefix "/events/:ids") :ids #"([0-9+],?)+"] [ids]
       (-> (fn [request] (client-api/fetch-event request ids))
           (util/wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (OPTIONS (str url-prefix "/events") []
       (-> #'success-handler
           (util/wrap-cors-headers "GET" "POST")
           (wrap-request-logger)))
  (GET (str url-prefix "/events") []
       (-> #'client-api/fetch-events
           (util/wrap-cors-headers "GET" "POST")
           (wrap-request-logger)))

  ;; Tracker-API
  (POST (str url-prefix "/events") []
        (-> #'tracker-api/handle-create-event
	    (wrap-request-logger)
            ))
 )