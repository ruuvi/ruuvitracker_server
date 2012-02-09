(ns ruuvi-server.api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.models.entities :as db])
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:require [clj-json.core :as json])
  (:use ring.middleware.json-params)
  (:use ring.middleware.params)
  (:use ring.middleware.session)
  (:use ring.middleware.cookies)
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:import [org.apache.commons.codec.binary Hex])
  (:import [java.security MessageDigest])
  (:import [org.joda.time.format DateTimeFormat DateTimeFormatter])
  )

(def date-time-formatter (DateTimeFormat/forPattern "YYYY-MM-dd'T'HH:mm:ss.SSS"))
(defn timestamp [] (.toString (new org.joda.time.DateTime)))

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


(defn- map-api-event-to-internal [params]
  (let [date-time (.parseDateTime date-time-formatter (params :time))]
    (merge params {:event_time (.toDate date-time) :tracker_identifier (params :trackerid)})
    )
  )

;; TODO handle authentication correctly
(defn create-event [request]
  (if (request :authenticated-tracker)
    (try
      (let [internal-event (map-api-event-to-internal (request :params))]
        (db/create-event internal-event)
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

(defn fetch-trackers [request]
  "not implemented yet")

(defn fetch-events [request]
  "not implemented yet")

(defn compute-mac [params tracker]
  (let [secret (tracker :shared_secret)
        request-mac (params "mac")
        ; sort keys alphabetically
        sorted-keys (sort (keys params))
        ; remove :mac key
        included-keys (filter (fn [param-key]
                                (not= "mac" param-key))
                              sorted-keys)
        ; make included-keys a vector and convert to non-lazy list
        param-keys (vec included-keys)]
    
   ; concatenate keys, values and separators. also add shared secret
   (let [value (str (apply str (for [k param-keys]
                                  (str (name k) ":" (params k) "|")
                                  )))
         value-with-shared-secret (str value secret)
         messageDigester (MessageDigest/getInstance "SHA-1")]
      (let [computed-mac (.digest messageDigester (.getBytes value-with-shared-secret "ASCII"))
            computed-mac-hex (Hex/encodeHexString computed-mac)]
        (debug (str  "orig-mac "(str request-mac) " computed mac " (str computed-mac-hex)) )
        computed-mac-hex
        ))))

(defn wrap-create-event-tracker
  "Find track with :trackerid and set value to :tracker key"
  [app]
  (fn [request]
    (let [params (request :params)
          trackerid (params :trackerid)
          tracker (db/get-tracker-by-identifier trackerid)]
      (if tracker
        (app (merge request {:tracker tracker}))
        (app request)))))

(defn wrap-create-event-auth
"Marks authentication status to request:
Sets keys
- :authenticated-tracker, if properly authenticated.
- :not-authenticated, if client chooses not to use autentication.
- :unknown-tracker, if client tracker is not known in database.
- :authentication-failed, autentication was attempted, but macs do not match.
"
  [app]
  (fn [request]
    (let [params (request :form-params)
          tracker (request :tracker)]
      (cond
       (not (params "mac")) (do
                              (debug "Client does not use authentication")
                              (app (merge request {:not-authenticated true} )))
       (not tracker) (do
                       (debug "Tracker does not exist in system")
                       (app (merge request {:unknown-tracker true})))
                       
       :else
       (let [computed-mac (compute-mac params (request :tracker))
             request-mac (params "mac")]
         (if (= computed-mac request-mac)
           (do
             (debug "Tracker is authenticated successfully")
             (app (merge request {:authenticated-tracker true})))
           (do
             (debug "Tracker failed authentication")
             (app (merge request {:authentication-failed true})))
           ))))))

(defn wrap-request-logger
  "Logs each incoming request"
  [app request-name]
  (fn [request]
    (info (str "Received " request-name "-request from " (request :remote-addr) ))
    (app request)
    ))

(defroutes api-routes
  (GET "/ping" [] (-> #'ping
                      (wrap-request-logger "ping")
                      ))
  (POST "/events" [] (-> #'create-event
                        (wrap-request-logger "create-event")
                        (wrap-create-event-auth)
                        (wrap-create-event-tracker)
                        ))
  (GET "/trackers" [] (-> #'fetch-trackers
                          (wrap-request-logger "fetch-trackers")
                          (wrap-json-params)
                          ))
  (GET "/events" [] (-> #'fetch-events
                        (wrap-request-logger "fetch-events")
                        (wrap-json-params)
                        )))
