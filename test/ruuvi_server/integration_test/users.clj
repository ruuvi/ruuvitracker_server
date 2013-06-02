(ns ruuvi-server.integration-test.users
  (:require  [ruuvi-server.database.entities :as entities]
             [ruuvi-server.configuration :as conf]
             [cheshire.core :as json]
             [clj-http.client :as http]
             [clojure.java.jdbc :as sql]
             [ruuvi-server.user-api :as api]
             [ruuvi-server.database.user-dao :as dao]
             [ruuvi-server.database.event-dao :as event-dao]
             [cheshire.core :as json])
  (:use [ruuvi-server.launcher :only (start-server migrate)]
        [clojure.tools.logging :only (debug info warn error)]
        [ruuvi-server.integration-test.itest-utils :only (http-get http-post json-get)]
        [midje.sweet :only (fact throws truthy falsey contains just)]
        [clj-time.core :only (date-time)]
        [clj-time.coerce :only (to-timestamp)]
        [lamina.core :only (enqueue close receive read-channel join wait-for-result)]
        ))
(def server-port 19998)
(def pre-config {:environment :dev

             :database {:classname "org.h2.Driver"
                        :subprotocol "h2"
                        ;; DB_CLOSE_DELAY keeps db around until JVM
                        ;; shuts down
                        :subname "mem:integration_users;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
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

(conf/init-config pre-config)

(api/db-conn)
(info "execute database migrations...")
(migrate config [:forward])
(info "database done")

(info "define some helper functions")
(defn parse-body [data]
  (let [b (:body data)]
    (json/parse-string b true)
  ))
(defn get-first [ring-response key]
  (first (key (json/parse-string (:body ring-response) true))))

(info "creating some users via user-api")
(fact (parse-body (api/fetch-users {} [1 2])) => {:users []})

(def new-user1 {:username "zorro"
                :email "zorro@example.com"
                :name "Mr. Zorro"
                :password "verysecret"})

(api/create-user {:params {:user new-user1}})

(def user1 (get-first (api/fetch-users {} [1]) :users))

(fact user1 => (just {:id 1
                      :username "zorro"
                      :name "Mr. Zorro"
                      :created_on truthy
                      :updated_on truthy}))

(def new-user2 {:username "bean"
                :email "bean@example.com"
                :name "Mr. Bean"
                :password "sosecret"})

(api/create-user {:params {:user new-user2}})

(def user2 (get-first (api/fetch-users {} [2]) :users))
(fact user2 => (just {:id 2
                      :username "bean"
                      :name "Mr. Bean"
                      :created_on truthy
                      :updated_on truthy}))

(info "creating some groups via user-api")
(api/create-group {:params {:group {:name "group a"}}})

(def group1 (get-first (api/fetch-groups {} [1]) :groups))
(fact group1 => (just {:id 1
                       :name "group a"
                       :created_on truthy
                       :updated_on truthy}))
                    
(api/create-group {:params {:group {:name "group b"}}})
(api/create-group {:params {:group {:name "group c"}}})

(def group2 (get-first (api/fetch-groups {} [2]) :groups))
(fact group2 => (just {:id 2
                       :name "group b"
                       :created_on truthy
                       :updated_on truthy}))

(def group3 (get-first (api/fetch-groups {} [3]) :groups))
(fact group3 => (just {:id 3
                       :name "group c"
                       :created_on truthy
                       :updated_on truthy}))

(info "starting server...")
(def kill-server (start-server config))
(info "server started")



(let [users (json-get server-port "/api/v1-dev/users/1,42")
      user (first (:users users))]
  (fact user => (just {:id 1
                       :username "zorro"
                       :name "Mr. Zorro"
                       :created_on truthy
                       :updated_on truthy})))

(let [groups (json-get server-port "/api/v1-dev/groups/1,42")
      group (first (:groups groups))]
  (fact group => (just {:id 1
                        :name "group a"
                        :created_on truthy
                        :updated_on truthy})))



(info "stopping server...")
(kill-server)
(info "server stopped")
