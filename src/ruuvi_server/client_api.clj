(ns ruuvi-server.client-api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.models.entities :as db])
  (:require [clj-json.core :as json])
  (:require [clojure.walk :as walk])
  (:import org.joda.time.DateTime)
)

(defn- object-to-string
  "convert objects in map to strings, assumes that map is flat"
  [data-map]
  (walk/prewalk (fn [item]
                 (cond (instance? java.util.Date item) (.print util/date-time-formatter (DateTime. item))
                       :else item)
                  ) data-map))

(defn- json-response
  "Formats data map as JSON" 
  [request data & [status]]
  (let [jsonp-function ((request :params) :jsonp)
        converted-data (object-to-string data)
        body (if jsonp-function
              (str jsonp-function "(" (json/generate-string converted-data) ")")
              (json/generate-string converted-data))]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body body}))

(defn- ping [request]
  (json-response request {"ruuvi-tracker-protocol-version" "1"
                  "server-software" (str server-name "/" server-version)
                  "time" (util/timestamp)}))

(defn- string-to-ids [value]
  (let [strings (.split value ",")
        ids (map #(Integer/parseInt %) strings)]
    ids
    )    
  )

(defn fetch-trackers [request]
  (json-response request {:trackers (db/get-all-trackers)} ))

(defn fetch-tracker [request id-string]
  ;; TODO id-string may be also non numeric tracker_code?
  (json-response request {:trackers (db/get-trackers (string-to-ids id-string))})
  )

(defn- parse-events-search-params [request]
  ;; parse sinceEventTime, sinceStoreTime, (tracker_code, tracker_id may be multiple)
  
  )
(defn fetch-events [request]
  (let [found-events (db/get-all-events)]
    (json-response request {:events found-events})))

(defn fetch-event [request id-string]
  (json-response request {:trackers (db/get-events (string-to-ids id-string))})
  )

