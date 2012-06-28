(ns ruuvi-server.tracker-api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.tracker-security :as sec])
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.database.entities :as db])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use ring.middleware.json-params)
  (:use ring.middleware.keyword-params)
  (:use ring.middleware.params)
  )

(defn- map-api-event-to-internal [params]
  (let [date-time (util/parse-timestamp (params :time))
        latitude (util/parse-coordinate (params :latitude))
        longitude (util/parse-coordinate (params :longitude))
        accuracy (util/parse-decimal (params :accuracy))
        altitude (util/parse-decimal (params :altitude))
        ]
    (merge params {:event_time date-time
                   :latitude latitude
                   :longitude longitude
                   :accuracy accuracy
                   :altitude altitude
                   })))

;; TODO handle authentication correctly
(defn- create-event
  "Checks if user is authenticated correctly and stores event to database.
TODO auth check should not be a part of this method.
"
  [request]
  (if (request :authenticated-tracker)
    (try
      (let [internal-event (map-api-event-to-internal (request :params))]
        (db/create-event internal-event)
        (info "Event stored")
        {:status 200
         :headers {"Content-Type" "text/plain"}
         :body "accepted"}
        )
      (catch Exception e
        (error "Error" e)
        {:status 500
         :headers {"Content-Type" "text/plain"}
         :body (str "Internal server error" (.getMessage e))}       
        ))
    {:status 401
     :headers {"Content-Type" "text/plain"}
     :body "not authorized"}
    ))

(defn- wrap-find-tracker
  "Find track with :tracker_code and set value to :tracker key"
  [app]
  (fn [request]
    (let [params (request :params)
          tracker-code (params :tracker_code)
          tracker (db/get-tracker-by-code tracker-code)]
      (if tracker
        (app (merge request {:tracker tracker}))
        (app request)))))

(defn- wrap-authentication-info
  "Marks authentication status to request:
Sets keys
- :authenticated-tracker, if properly authenticated.
- :not-authenticated, if client chooses not to use autentication.
- :unknown-tracker, if client tracker is not known in database.
- :authentication-failed, autentication was attempted, but macs do not match.
"
  [app]
  (fn [request]
    (let [params (request :params)
          tracker (request :tracker)]
      (app (merge request
                  (sec/authentication-status params tracker :mac))))))

(defn handle-create-event [request]
  ;; TODO modifications done to request at
  ;; higher levels do not affect anything here
  (-> 
   #'create-event
   (wrap-authentication-info)
   (wrap-find-tracker)
   (wrap-keyword-params)
   (wrap-params)
   (wrap-json-params)
   (util/wrap-cors-headers "GET POST")
   ))