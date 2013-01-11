(ns ruuvi-server.integration-test.server
  (:require  [ruuvi-server.database.entities :as entities]
             [ruuvi-server.configuration :as conf]
             [clj-http.client :as http])
  (:use [ruuvi-server.launcher :only (start-server migrate)]
        [clojure.tools.logging :only (debug info warn error)]
        [ruuvi-server.database.event-dao]
        [midje.sweet :only (fact throws)]
        [clj-time.core :only (date-time)]
        [clj-time.coerce :only (to-timestamp)]
        )
  )

(def pre-config {:environment :dev

             :database {:classname "org.h2.Driver"
                        :subprotocol "h2"
                        ;; DB_CLOSE_DELAY keeps db around until JVM
                        ;; shuts down
                        :subname "mem:integration_tests;DB_CLOSE_DELAY=-1"
                        :user "sa"
                        :password ""             
                        }
             :server {
                      :type :standalone
                      :engine :aleph
                      :port 8888
                      :max-threads 80
                      :enable-gzip true
                      }
             :tracker-api {:require-authentication false
                           :allow-tracker-creation true
                           }
             :client-api {:default-max-search-results 100
                          :allowed-max-search-results 1000
                          }
             }
  )

(def config (conf/post-process-config :dev pre-config))
(conf/init-config config)

(info "initializing Korma...")
(entities/init config)
(info "Korma initialized")

(info "execute database migrations...")
(migrate config [:forward])
(info "database done")


(info "fetching stuff from empty database should return empty results")
(fact (get-trackers [1 2 3]) => '())
(fact (get-all-trackers) => '())
(fact (get-tracker 42) => nil)
(fact (get-tracker-by-code "code") => nil)
(fact (get-event-sessions {:tracker_ids [1 2 3] :event_session_ids [4 5 6]}) => '())
(fact (get-extension-type-by-id 2) => nil)
(fact (get-extension-type-by-name "name") => nil)
(fact (get-event 2) => nil)
(fact (get-events [1 2 3]) => '())
(fact (get-all-events) => '())

(fact (search-events {}) => '())


;; trackers
(create-tracker "code1" "name1" "secret1" "password1")
(def tracker-1 (get-tracker-by-code "code1"))
(fact (:id tracker-1) => 1
      (:name tracker-1) => "name1"
      (:tracker_code tracker-1) => "code1"
      (:shared_secret tracker-1) => "secret1"
      (:password tracker-1) => "password1")

(fact (get-tracker 1) => tracker-1)

(create-tracker "code2" "name2" "secret2" "password2")
(def tracker-2 (get-tracker-by-code "code2"))
(fact (get-trackers [1 2 3]) => [tracker-1 tracker-2] )
(fact (get-all-trackers) => [tracker-1 tracker-2] )

;; events
(def event-time1 (date-time 2013 1 12 0 1 42 61))
(def event-data-1 {:event_time event-time1
                   :tracker_code "code1"
                   })
(create-event event-data-1)

(def event-1 (get-event 1))
(fact (:tracker_code event-1) => "code1"
      (:event_time event-1) => (to-timestamp event-time1)
      )

(def session-1 (first (get-event-sessions {:tracker-ids [(:id tracker-1)]
                                          })))
(fact (:session_code session-1) => "default")

(comment
(def event-data-2 {:session_code "session-code2"
                   :tracker_code "code2"
                   :latitude "23"
                   :longitude "61.0"
                   })

(create-event event-data-2)
(def event-2 (get-event 2))

(def session-2 (first (get-event-sessions {:tracker-ids [(:id tracker-2)]
                                          :event_session_ids [(:event_session_id event-2)]})))
(fact (:session_code session-2) => "session-code2")
)

;; return value of start-server, kills the server
(info "starting server...")
(def kill-server (start-server config))
(info "server started")

(info "stopping server...")
@(kill-server)
(info "server stopped")


