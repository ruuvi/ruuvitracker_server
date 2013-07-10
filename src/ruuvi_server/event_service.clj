(ns ruuvi-server.event-service
  "Client API implementation"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.event-dao :as db]
            [ruuvi-server.database.user-dao :as user-dao]
            [clojure.string :as string]
            [ruuvi-server.common :as common]
            [ruuvi-server.message :as message]
            [ruuvi-server.configuration :as conf]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [clojure.set :only (rename-keys)]
        )
  )

(defn- db-conn []
  (get-in (conf/get-config) [:database]))

(defn ping [request]
  {:body  {"ruuvi-tracker-protocol-version" "1"
           "server-software" (str common/server-name "/" common/server-version)
           "time" (parse/timestamp)}})

(defn auth-user-id [request]
  (-> request :session :user-id))

(defn fetch-trackers [request &[tracker-ids]]
  (let [user-id (auth-user-id request)
        trackers (user-dao/get-user-visible-trackers (db-conn) user-id tracker-ids)]
    {:body (message/select-trackers-data {:trackers trackers} )}))

(defn fetch-session [request]
  (let [tracker-ids (:tracker_ids request)
        session-ids (:event_session_ids request)
        ids (util/remove-nil-values {:tracker_ids tracker-ids
             :event_session_ids session-ids})
        ]
    {:body (message/select-event-sessions-data {:event_sessions (db/get-event-sessions ids)} )}))

(defn- parse-event-search-criterias [request]
  ;; TODO use letfn instead
  (defn- parse-date[key date-str]
    (when date-str
      ;; TODO move " " -> "+" to parse-timestamp
      ;; Timezone may contain a + char. Browser converts + to space in url encode. This reverts the change.
      (let [date-tmp (.replaceAll date-str " " "+")
            date (parse/parse-timestamp date-tmp)]
        (if date
          {key date}
          nil))))
  (let [params (request :params)
        user-id (auth-user-id request)
        maxResultsParam (params :maxResults)
        maxResults (when maxResultsParam {:maxResults (Integer/valueOf maxResultsParam) } )
        ;; TODO this simply ignores invalid values => not good, should throw exception instead
        eventTimeStart (parse-date :eventTimeStart (params :eventTimeStart))
        storeTimeStart (parse-date :storeTimeStart (params :storeTimeStart))
        eventTimeEnd (parse-date :eventTimeEnd (params :eventTimeEnd))
        storeTimeEnd (parse-date :storeTimeEnd (params :storeTimeEnd))
        trackerIds {:trackerIds (user-dao/filter-visible-trackers (db-conn) user-id (request :tracker_ids))}
        sessionIds {:sessionIds (user-dao/filter-visible-sessions (db-conn) user-id (request :event_session_ids))}
        orderBy {:orderBy (request :order-by)}
        ]
    (util/remove-nil-values (merge {} trackerIds sessionIds eventTimeStart eventTimeEnd
           storeTimeStart storeTimeEnd maxResults orderBy))
    ))

(defn fetch-events [request]
  (let [query-params (parse-event-search-criterias request)
        found-events (db/search-events query-params)]
    {:body (message/select-events-data {:events found-events})}))

(defn fetch-event [request id-string]
  {:body (message/select-events-data {:events (db/get-events id-string)})}
  )

(defn create-tracker
  "Creates a new tracker.
Expected content in params:
 {tracker: {name: \"abc\", code: \"foo\", shared_secret: \"foo\", password: \"foo\" :public false}}
"
  [request]

  (let [params (:params request)
        tracker (:tracker params)
        name (string/trim (or tracker :name ""))
        code (string/trim (get tracker :code ""))
        shared-secret (string/trim (get tracker :shared_secret ""))
        password (string/trim (get tracker :password ""))
        description (string/trim (get tracker :description "")) 
        public (parse/parse-boolean (get tracker :public false))]
    (cond
     (not tracker) (util/error-response request "tracker element missing" 400)
     (not name) (util/error-response request "name element missing" 400)
     (not code) (util/error-response request "code element missing" 400)
     (not shared-secret) (util/error-response request "shared_secret element missing" 400)
     :default
     (let [existing-tracker (db/get-tracker-by-code code)
           owner-id (auth-user-id request)]
       (if existing-tracker
         (util/error-response request "tracker already exists" 409)
         (let [new-tracker (db/create-tracker owner-id code name shared-secret password description public)]
           (util/response request {:result "ok" :tracker (message/select-tracker-data new-tracker)}))
         )))))
