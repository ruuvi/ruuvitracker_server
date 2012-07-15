(ns ruuvi-server.client-api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.database.event-dao :as db])
  (:require [clj-json.core :as json])
  (:require [clojure.walk :as walk])
  (:import org.joda.time.DateTime)
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.set :only (rename-keys)])
  )

(defn- object-to-string
  "convert objects in map to strings, assumes that map is flat"
  [data-map]
  (walk/prewalk (fn [item]
                  (cond (instance? java.util.Date item) (.print util/date-time-formatter (DateTime. item))
                        (instance? java.math.BigDecimal item) (str item)
                        :else item)
                  ) data-map))

(defn- select-extension-data
  "Convert extension_data to name value pairs. Skip extension data if name is missing."
  [extension-data]
  
  (filter identity
          (map (fn [data]
                 (when-let [name (:name data)]
                   {name (:value data)}))
               extension-data)))
  
(defn- select-location-data [location-data]
  (when location-data
    (let [selected-data (select-keys location-data
                                     [:longitude :latitude :altitude
                                      :satellite_count :accuracy])]
      (util/remove-nil-values selected-data)
      )))

(defn- select-event-data [event-data]
  (let [selected-data (select-keys event-data
                                   [:id :event_time :tracker_id
                                    :created_on])
        renamed-data (util/modify-map selected-data
                                 {:created_on :store_time}
                                 {:id str
                                  :tracker_id str})
        location-data (select-location-data
                       (get (event-data :event_locations)
                            0))
        extension-data (select-extension-data
                        (event-data :event_extension_values))]

    (util/remove-nil-values (merge renamed-data
                                   {:location location-data
                                    :extension_values extension-data}))
    ))

(defn- select-event-data [event-data]
  (let [selected-data (select-keys event-data
                                   [:id :event_time :tracker_id
                                    :created_on])
        renamed-data (util/modify-map selected-data
                                 {:created_on :store_time}
                                 {:id str
                                  :tracker_id str})
        location-data (select-location-data
                       (get (event-data :event_locations)
                            0))
        extension-data (select-extension-data
                        (event-data :event_extension_values))]
    (util/remove-nil-values (merge renamed-data
                                   {:location location-data
                                    :extension_values extension-data}))
  ))

(defn- select-events-data [data-map]
  {:events
   (map select-event-data (data-map :events))}
  )

(defn- select-tracker-data [data-map]
  (let [selected (select-keys data-map [:id :tracker_code :name
                                        :latest_activity :created_on])
        renamed (util/modify-map selected nil {:id str})]
    (util/remove-nil-values renamed)))
  
(defn- select-trackers-data [data-map]
  {:trackers (map select-tracker-data (data-map :trackers))})

(defn- json-response
  "Formats data map as JSON" 
  [request data & [status]]
  (let [jsonp-function ((request :params) :jsonp)
        converted-data (object-to-string data)
        body (if jsonp-function
              (str jsonp-function "(" (json/generate-string converted-data) ")")
              (json/generate-string converted-data))]
  {:status (or status 200)
   :headers {"Content-Type" "application/json;charset=UTF-8"}
   :body body}))

(defn- ping [request]
  (json-response request {"ruuvi-tracker-protocol-version" "1"
                  "server-software" (str server-name "/" server-version)
                  "time" (util/timestamp)}))

(defn- string-to-ids [value]
  (when value
    (let [strings (.split value ",")
          ids (map #(Integer/parseInt %) strings)]
      ids
    )))

(defn fetch-trackers [request]
  (json-response request (select-trackers-data {:trackers (db/get-all-trackers)} )))

(defn fetch-tracker [request id-string]
  (json-response request (select-trackers-data {:trackers (db/get-trackers (string-to-ids id-string))} )))

(defn- parse-event-search-criterias [request]
  (defn- parse-date[key date-str]
    (when date-str
      ;; Timezone may contain a + char. Browser converts + to space in url encode. This reverts the change.
      (let [date-tmp (.replaceAll date-str " " "+")
            date (util/parse-timestamp date-tmp)]
        (if date
          {key date}
          nil))))
  (let [params (request :params)
        maxResultsParam (params :maxResults)
        maxResults (when maxResultsParam {:maxResults (Integer/valueOf maxResultsParam) } )
        ;; TODO this simply ignores invalid values => not good, should throw exception instead
        eventTimeStart (parse-date :eventTimeStart (params :eventTimeStart))
        storeTimeStart (parse-date :storeTimeStart (params :storeTimeStart))
        eventTimeEnd (parse-date :eventTimeEnd (params :eventTimeEnd))
        storeTimeEnd (parse-date :storeTimeEnd (params :storeTimeEnd))
        trackerIds {:trackerIds (string-to-ids (request :tracker_ids))}
        ]
    (merge {} trackerIds eventTimeStart eventTimeEnd
           storeTimeStart storeTimeEnd maxResults)
    ))

(defn fetch-events [request ]
  (let [query-params (parse-event-search-criterias request)
        found-events (db/search-events query-params)]
    (json-response request
                   (select-events-data {:events found-events}))))

(defn fetch-event [request id-string]
  (json-response request
                 (select-events-data {:events (db/get-events (string-to-ids id-string))}))
  )

