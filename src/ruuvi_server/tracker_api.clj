(ns ruuvi-server.tracker-api
  (:require [ruuvi-server.tracker-security :as sec]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.event-dao :as db]
            [ruuvi-server.configuration :as conf]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [ring.middleware.json-params :only (wrap-json-params)]
        [ring.middleware.keyword-params :only (wrap-keyword-params)]
        [ring.middleware.params :only (wrap-params)]
        )
  )

(defn- map-api-event-to-internal
  "Converts incoming data to internal presentation."
  [params]
  (let [date_time (when (params :time) (parse/parse-timestamp (params :time)))
        latitude (when (params :latitude) (parse/parse-coordinate (params :latitude)))
        longitude (when (params :longitude) (parse/parse-coordinate (params :longitude)))
        horizontal_accuracy (when (params :accuracy) (parse/parse-decimal (params :accuracy)))
        vertical_accuracy (when (params :vertical-accuracy) (parse/parse-decimal (params :vertical-accuracy)))
        speed (when (params :speed) (parse/parse-decimal (params :speed)))
        heading (when (params :heading) (parse/parse-decimal (params :heading)))
        satellite_count (when (params :satellite-count) (parse/parse-decimal (params :satellite-count)))
        altitude (when (params :altitude) (parse/parse-decimal (params :altitude)))
        ]
    ;; TODO use select-keys
    (info (:speed params))
    (info params)
    (merge params {:event_time date_time
                   :latitude latitude
                   :longitude longitude
                   :horizontal_accuracy horizontal_accuracy
                   :vertical_accuracy vertical_accuracy
                   :altitude altitude
                   :heading heading
                   :speed speed
                   :satellite_count satellite_count
                   })))

(defn- allowed-create-event?
  "* Correctly authenticated user is always allowed.
* If authentication failed, user is not allowed.
* Unknown (= no tracker found with tracker code) and users not using authentication are allowed depending on configuration."
  [request]
  (let [tracker-conf (:tracker-api (conf/get-config))]
    (cond (request :authenticated-tracker)
          true
          
          (and (request :not-authenticated)
               (not (:require-authentication tracker-conf)))
          true

          (and (request :unknown-tracker)
               (:allow-tracker-creation tracker-conf))
          true
                     
          :else false
          )))

(defn- create-event
  "Checks if user is authenticated correctly and stores event to database.
TODO auth check should not be a part of this method.
"
  [request]
  (if (allowed-create-event? request)
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
        ;; use (error) to printStackTrace
        (.printStackTrace e)
        {:status 500
         :headers {"Content-Type" "text/plain"}
         :body (str "Internal server error" (.getMessage e))}       
        ))
    {:status 401
     :headers {"Content-Type" "text/plain"}
     :body "not authorized"}
    ))

(defn- wrap-find-tracker
  "Find track with `:tracker_code` and set value to `:tracker` key"
  [app]
  (fn [request]
    (let [params (request :params)
          tracker-code (params :tracker_code)
          tracker (db/get-tracker-by-code tracker-code)]
      (if tracker
        (app (merge request {:tracker tracker}))
        (app request)))))

(defn- wrap-authentication-info
  "Marks authentication status to request. Sets keys:

* `:authenticated-tracker`, if properly authenticated.
* `:not-authenticated`, if client chooses not to use autentication.
* `:unknown-tracker`, if client tracker is not known in database.
* `:authentication-failed`, autentication was attempted, but macs do not match.
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
   ))