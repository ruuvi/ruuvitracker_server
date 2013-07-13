(ns ruuvi-server.tracker-service
  (:require [ruuvi-server.tracker-security :as sec]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.event-dao :as db]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.websocket-api :as websocket]
            [ruuvi-server.api-schema :as schema]
            [ruuvi-server.util :as util]
            [ruuvi-server.message :as message]
            [ruuvi-server.middleware :as middleware])
  (:use [clojure.tools.logging :only (debug info warn error)]
        [ring.middleware.json :only (wrap-json-params)]
        [ring.middleware.keyword-params :only (wrap-keyword-params)]
        [ring.middleware.params :only (wrap-params)] ))

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
      (let [internal-event (util/convert (request :params) schema/new-event-conversion)
            created-event (db/create-event internal-event)
            use-websocket (get-in (conf/get-config) [:server :websocket] false)]
        (info "Event stored")
        ;; TODO format event
        (when use-websocket
          (websocket/publish-event (:tracker_id created-event)
                                   (message/select-events-data {:events [created-event]}) ))
        
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
   create-event
   (wrap-authentication-info)
   (wrap-find-tracker)
   (middleware/wrap-validate-schema schema/new-single-event-schema)
   ))
