(ns ruuvi-server.client-api
  "Client API implementation"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.event-dao :as db]
            [clojure.string :as string]
            [ruuvi-server.common :as common]
            [ruuvi-server.message :as message]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [clojure.set :only (rename-keys)]
        )
  )

(defn- ping [request]
  (util/json-response request {"ruuvi-tracker-protocol-version" "1"
                  "server-software" (str common/server-name "/" common/server-version)
                  "time" (parse/timestamp)}))

(defn- string-to-ids [value]
  (when value
    (let [strings (.split value ",")
          ids (map #(Integer/parseInt %) strings)]
      ids
    )))

(defn fetch-trackers [request]
  (util/json-response request (message/select-trackers-data {:trackers (db/get-all-trackers)} )))

(defn fetch-tracker [request id-string]
  (util/json-response request (message/select-trackers-data {:trackers (db/get-trackers (string-to-ids id-string))} )))

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
    (util/json-response request (message/select-event-sessions-data {:event_sessions (db/get-event-sessions ids)} ))))                     

(defn- parse-event-search-criterias [request]
  (defn- parse-date[key date-str]
    (when date-str
      ;; Timezone may contain a + char. Browser converts + to space in url encode. This reverts the change.
      (let [date-tmp (.replaceAll date-str " " "+")
            date (parse/parse-timestamp date-tmp)]
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
    (util/json-response request
                   (message/select-events-data {:events found-events}))))

(defn fetch-event [request id-string]
  (util/json-response request
                 (message/select-events-data {:events (db/get-events (string-to-ids id-string))}))
  )

(defn create-tracker
  "Creates a new tracker.
Expected content in params:
 {tracker: {name: \"abc\", code: \"foo\", shared_secret: \"foo\", password: \"foo\"}}
"
  [request]
  ;; TODO use validation framework
  (let [params (:params request)
        tracker (:tracker params)
        name (string/trim (or (:name tracker) ""))
        code (string/trim (or (:code tracker) ""))
        shared-secret (string/trim (or (:shared_secret tracker) ""))
        password (string/trim (or (:password tracker) ""))
        ]
    (cond
     (not tracker) (util/json-error-response request "tracker element missing" 400)
     (not name) (util/json-error-response request "name element missing" 400)
     (not code) (util/json-error-response request "code element missing" 400)
     (not shared-secret) (util/json-error-response request "shared_secret element missing" 400)
     :default
     (let [existing-tracker (db/get-tracker-by-code code)]
       (if existing-tracker
         (util/json-error-response request "tracker already exists" 409)
         (let [new-tracker (db/create-tracker code name shared-secret password)]
           (util/json-response request {:result "ok" :tracker (message/select-tracker-data new-tracker)}) 
           )
         ))
     )
    ))