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

(defn- wrap-request-logger
  "Logs each incoming request"
  [app]
  (fn [request]
    (swap! request-counter inc)
    (let [request-method (:request-method request)
          uri (:uri request)
          query-params (:query-params request)
          start (System/currentTimeMillis)]
      (info (str "REQUEST:" @request-counter)
            request-method uri ":query-params" query-params)
      (let [response (app request)
            duration (- (System/currentTimeMillis) start)
            status (:status response)]
        (info (str "RESPONSE:" @request-counter)
              (str "duration: " duration " msec")
              status)
        response
        )
    )))

(def url-prefix "/v1-dev")

(defn- success-handler[request]
  {:status 200
   :headers {"Content-Type" "application/json;charset=UTF-8"}})

(defn- wrap-cors-headers
  "http://www.w3.org/TR/cors/"
  [app & methods]
  (fn [request]
    (let [response (app request)
          options (apply str (interpose ", " (conj methods "OPTIONS")))
          cors-response
          (merge response
                 {:headers   
                  (merge (:headers response)
                         {"Access-Control-Allow-Origin" "*"
                          "Access-Control-Allow-Headers" "X-Requested-With"
                          "Access-Control-Allow-Methods" options})})]
      cors-response
      )))

;; TODO do some macro thing that creates automatically OPTIONS route
(defroutes api-routes
  (OPTIONS (str url-prefix "/ping") []
       (-> #'success-handler
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET (str url-prefix "/ping") []
       (-> #'client-api/ping
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (POST (str url-prefix "/events") []
        (-> #'tracker-api/handle-create-event
            (wrap-request-logger)))
  
  (OPTIONS [(str url-prefix "/trackers/:ids") :ids #"([0-9]+,?)+"] [ids]
       (-> #'success-handler
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET [(str url-prefix "/trackers/:ids") :ids #"([0-9]+,?)+"] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (OPTIONS [(str url-prefix "/trackers/:ids/events") :ids #"([0-9]+,?)+"] [ids]
       (-> #'success-handler
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET [(str url-prefix "/trackers/:ids/events") :ids #"([0-9]+,?)+"] [ids]
       (-> (fn [request] (client-api/fetch-events (merge request {:tracker_ids ids})))
           (wrap-cors-headers "GET")
           (wrap-request-logger "fetch events for -trackers")))
  
  (OPTIONS (str url-prefix "/trackers") []
       (-> #'success-handler
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET (str url-prefix "/trackers") []
       (-> #'client-api/fetch-trackers
           (wrap-cors-headers "GET")
           (wrap-request-logger "fetch-trackers")))

  (OPTIONS [(str url-prefix "/events/:ids") :ids #"([0-9+],?)+"] [ids]
       (-> #'success-handler
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET [(str url-prefix "/events/:ids") :ids #"([0-9+],?)+"] [ids]
       (-> (fn [request] (client-api/fetch-event request ids))
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  
  (OPTIONS (str url-prefix "/events") []
       (-> #'success-handler
           (wrap-cors-headers "GET")
           (wrap-request-logger)))
  (GET (str url-prefix "/events") []
       (-> #'client-api/fetch-events
           (wrap-cors-headers "GET")
           (wrap-request-logger)
           )))
