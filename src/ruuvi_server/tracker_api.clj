(ns ruuvi-server.tracker-api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.database.entities :as db])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:import [java.security MessageDigest])
  (:import [org.apache.commons.codec.binary Hex])
  )

(defn- map-api-event-to-internal [params]
  (let [date-time (.parseDateTime util/date-time-formatter (params :time))
        latitude (when (params :latitude)
                   (util/nmea-to-decimal (params :latitude)))
        longitude (when (params :longitude)
                    (util/nmea-to-decimal (params :longitude)))
        ]
    (merge params {:event_time date-time
                   :latitude latitude
                   :longitude longitude
                   })))

;; TODO handle authentication correctly
(defn- create-event [request]
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

(defn- compute-mac [params tracker]
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

(defn- wrap-create-event-tracker
  "Find track with :tracker_code and set value to :tracker key"
  [app]
  (fn [request]
    (let [params (request :params)
          tracker-code (params :tracker_code)
          tracker (db/get-tracker-by-code tracker-code)]
      (if tracker
        (app (merge request {:tracker tracker}))
        (app request)))))

(defn- wrap-create-event-auth
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


(defn handle-create-event [req]
  (-> #'create-event
      (wrap-create-event-auth)
      (wrap-create-event-tracker)
      ))
