(ns ruuvi-server.client-api
  (:use ruuvi-server.common)
  (:require [ruuvi-server.util :as util])
  (:require [ruuvi-server.database.event-dao :as db])
  (:require [clj-json.core :as json])
  (:require [clojure.walk :as walk])
  (:import org.joda.time.DateTime)
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.set :only (rename-keys)])
  (:require [clojure.string :as string])
  )

(defn- object-to-string
  "Convert objects in map to strings, assumes that map is flat"
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
                                    :event_session_id :created_on])
        renamed-data (util/modify-map selected-data
                                      {:created_on :store_time}
                                      {})
        renamed-data (util/stringify-id-fields renamed-data)
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

(defn- select-event-session-data [session-data]
  (let [selected-data (select-keys session-data
                                   [:id :tracker_id :session_code
                                    :first_event_time :latest_event_time])
        selected-data (util/stringify-id-fields selected-data)]
    (util/remove-nil-values selected-data)))
                                    
(defn- select-event-sessions-data [data-map]
  {:sessions
   (map select-event-session-data (data-map :event_sessions))})

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

(defn- json-error-response
  [request message status]
  (let [body {:error {:message message}}]
    (json-response request body status)))

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

(defn fetch-session [request]
  (let [tracker-id-list (:tracker_ids request)
        session-id-list (:event_session_ids request)
        tracker-ids (when tracker-id-list
                      (string-to-ids tracker-id-list))
        session-ids (when session-id-list
                      (string-to-ids session-id-list))
        ids (util/remove-nil-values {:tracker_ids tracker-ids
             :event_session_ids session-ids})
        ]
    (json-response request (select-event-sessions-data {:event_sessions (db/get-event-sessions ids)} ))))                     


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
        sessionIds {:sessionIds (string-to-ids (request :event_session_ids))}
        orderBy {:orderBy (request :order-by)}
        ]
    (util/remove-nil-values (merge {} trackerIds sessionIds eventTimeStart eventTimeEnd
           storeTimeStart storeTimeEnd maxResults orderBy))
    ))

(defn fetch-events [request]
  (let [query-params (parse-event-search-criterias request)
        found-events (db/search-events query-params)]
    (json-response request
                   (select-events-data {:events found-events}))))

(defn fetch-event [request id-string]
  (json-response request
                 (select-events-data {:events (db/get-events (string-to-ids id-string))}))
  )

;; {tracker: {name: "abc", code: "foo", shared_secret: "foo"}}
;;
;;
(defn create-tracker [request]
  ;; TODO use validation framework
  (let [params (:params request)
        tracker (:tracker params)
        name (string/trim (or (:name tracker) ""))
        code (string/trim (or (:code tracker) ""))
        shared-secret (string/trim (or (:shared_secret tracker) ""))
        ]
     (prn request)
    (cond

     (not tracker) (json-error-response request "tracker element missing" 400)
     (not name) (json-error-response request "name element missing" 400)
     (not code) (json-error-response request "code element missing" 400)
     (not shared-secret) (json-error-response request "shared_secret element missing" 400)
     :default
     (let [existing-tracker (db/get-tracker-by-code code)]
       (if existing-tracker
         (json-error-response request "tracker already exists" 409)
         (let [new-tracker (db/create-tracker code name shared-secret)]
           (json-response request {:result "ok" :tracker (select-tracker-data new-tracker)}) 
           )
         ))
     )
    ))