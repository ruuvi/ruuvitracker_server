(ns ruuvi-server.integration-test.server
  (:require  [ruuvi-server.database.entities :as entities]
             [ruuvi-server.configuration :as conf]
             [cheshire.core :as json]
             [clj-http.client :as http]
             [aleph.http.websocket :as websocket]
             [ruuvi-server.database.load-example-data :as example-data])
  (:use [ruuvi-server.launcher :only (start-server migrate)]
        [clojure.tools.logging :only (debug info warn error)]
        [ruuvi-server.database.event-dao]
        [ruuvi-server.integration-test.itest-utils :only (http-get http-post json-get)]
        [midje.sweet :only (fact throws truthy falsey)]
        [clj-time.core :only (date-time)]
        [clj-time.coerce :only (to-timestamp)]
        [lamina.core :only (enqueue close receive read-channel join wait-for-result)]
        ))

(def server-port 18888)
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
                      :port server-port
                      :max-threads 80
                      :websocket true
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

(info "creating trackers")
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

(info "creating events")
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

(def event-data-3 {:session_code "session-code2"
                   :tracker_code "code2"
                   :latitude "23"
                   :longitude "61.0"
                   :X-foo "foo"
                   :X-bar "bar"
                   :annotation "anno"
                   })

(create-event event-data-3)
(def event-3 (get-event 3))

(let [location (first (:event_locations event-3))]
  (fact (:latitude location) => 23M
        (:longitude location) => 61.0M))

(def session-3 (first (get-event-sessions {:tracker-ids [(:id tracker-2)]
                                           :event_session_ids [(:event_session_id event-3)]})))
(fact session-3 => session-2)

;; server must be started in try block
;; otherwise failing test will leave server running
(try

  ;; return value of start-server, kills the server
  (info "starting server...")
  (def kill-server (start-server config))
  (info "server started")
  
  
  (fact (http-get server-port "/") => truthy)
  (let [pong (json-get server-port "/api/v1-dev/ping")]
    (fact (:ruuvi-tracker-protocol-version pong) => "1"
          (:server-software pong) => string?
          (:time pong) => string?)
    )
  
  (info "Test fetching single events")
  (let [events (json-get server-port "/api/v1-dev/events/1")
        event1 (get-in events [:events 0])]
    (fact (:store_time event1) => string?
          (:event_time event1) => string?
          (:id event1) => "1"
          (:tracker_id event1) => "1"))
  
  (let [events (json-get server-port "/api/v1-dev/events/2")
        event (get-in events [:events 0])]
    (fact (:store_time event) => string?
          (:event_time event) => string?
          (:id event) => "2"
          (:tracker_id event) => "2"
          (get-in event [:location :latitude]) => "23"
          (get-in event [:location :longitude]) => "61.0"
          ))
  
  (defn create-event-via-api [tracker-code shared-secret data]
    (let [body (assoc data :tracker_code tracker-code) ]
      (http-post server-port "/api/v1-dev/events" body) ))
  
  (info "Test WebSocket connection")
  (def ws-connection @(websocket/websocket-client {:url
                                                   (str "ws://localhost:" server-port "/api/v1-dev/websocket")}))
  
  (enqueue ws-connection (json/generate-string {:subscribe :trackers :ids [1 2]}))
  
  (create-event-via-api (:tracker_code tracker-1)
                        "verysecret"
                        {:version "1" :latitude 61M :longitude 23M})
  
  (def result (wait-for-result (read-channel ws-connection) 1000))
  (let [result-json (json/parse-string result true)
        result-event (first (:events result-json ))]
    (fact (:tracker_id result-event) => "1"
          (:id result-event) => "4"))
  
  (close ws-connection)
  
  (finally
    (info "Stopping server...")
    @(kill-server)
    (info "Server stopped")
    ))
(info "Create example data")
(example-data/create-test-trackers)
(fact (:name (get-tracker-by-code "123")) => "Murre-tracker")
