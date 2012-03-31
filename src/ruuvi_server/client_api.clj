(ns ruuvi-server.client-api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.database.entities :as db])
  (:require [clj-json.core :as json])
  (:require [clojure.walk :as walk])
  (:import org.joda.time.DateTime)
)

(defn- object-to-string
  "convert objects in map to strings, assumes that map is flat"
  [data-map]
  (walk/prewalk (fn [item]
                  (cond (instance? java.util.Date item) (.print util/date-time-formatter (DateTime. item))
                        (instance? java.math.BigDecimal item) (str item)
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

(defn- parse-event-search-criterias [request]
  (defn parse-date[key date-str]
    (when date-str
      ;; Timezone may contain a + char. Browser converts + to space in url encode. This reverts the change.
      (let [date-tmp (.replaceAll date-str " " "+")
            date (util/parse-date-time date-tmp)]
            (if date
              {key date}
              nil))))
  (let [params (request :params)
        ;; TODO this simply ignores invalid values => not good, should throw exception instead
        eventTimeStart (parse-date :eventTimeStart (params :eventTimeStart))
        createTimeStart (parse-date :createTimeStart (params :createTimeStart))
        ]
    (merge {} eventTimeStart createTimeStart)
    ))

(defn fetch-events [request]
  (let [query-params (parse-event-search-criterias request)
        found-events (db/search-events query-params)]
    (json-response request {:events found-events})))

(defn fetch-event [request id-string]
  (json-response request {:trackers (db/get-events (string-to-ids id-string))})
  )

