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

(defn- wrap-request-logger
  "Logs each incoming request"
  [app request-name]
  (fn [request]
    (info (str "Received " request-name "-request from " (request :remote-addr) ))
    (app request)
    ))

(def url-prefix "/v1-dev")

(defroutes api-routes
  (GET (str url-prefix "/ping") []
       (-> #'client-api/ping
           (wrap-request-logger "ping")
           ))
  (POST (str url-prefix "/events") []
        (-> #'tracker-api/handle-create-event
            (wrap-request-logger "create-event")
            ))
  (GET [(str url-prefix "/trackers/:ids") :ids #"([0-9]+,?)+"] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           (wrap-request-logger "fetch-trackers")
           ))
  (GET [(str url-prefix "/trackers/:ids/events") :ids #"([0-9]+,?)+"] [ids]
       (-> (fn [request] (client-api/fetch-events (merge request {:tracker_ids ids})))
           (wrap-request-logger "fetch events for -trackers")
           ))
  (GET (str url-prefix "/trackers") []
       (-> #'client-api/fetch-trackers
           (wrap-request-logger "fetch-trackers")
           ))
  (GET [(str url-prefix "/events/:ids") :ids #"([0-9+],?)+"] [ids]
       (-> (fn [request] (client-api/fetch-event request ids))
           (wrap-request-logger "fetch-trackers")
           ))                         
  (GET (str url-prefix "/events") []
       (-> #'client-api/fetch-events
           (wrap-request-logger "fetch-events")
           )))
