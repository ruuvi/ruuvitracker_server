(ns ruuvi-server.integration-test.server
  (:require  [ruuvi-server.database.entities :as entities]
             [ruuvi-server.configuration :as conf])
  (:use [ruuvi-server.launcher :only (start-server migrate)]
        [clojure.tools.logging :only (debug info warn error)]
        [ruuvi-server.database.event-dao]
        [midje.sweet :only (fact throws)]
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
(fact (get-event-sessions {:tracker_ids [1 2 3] :event_session_ids [4 5 6]}) => '())
(fact (get-extension-type-by-id 2) => nil)
(fact (get-extension-type-by-name "name") => nil)
(fact (get-event 2) => nil)
(fact (get-events [1 2 3]) => '())
(fact (get-all-events) => '())

(fact (search-events {}) => '())


;; return value of start-server, kills the server
(info "starting server...")
(def kill-server (start-server config))
(info "server started")

(info "stopping server...")
@(kill-server)
(info "server stopped")


