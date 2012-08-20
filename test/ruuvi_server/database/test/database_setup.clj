(ns ruuvi-server.database.test.database-setup
  "Functions to create database setup for unit-tests."
  (:use midje.sweet)
  (:require [ruuvi-server.configuration :as conf]
            [ruuvi-server.database.entities :as entities]
            [lobos.migrations :as migrations]
            ))

(defn setup-db-connection [db-name]
  (let [test-config {:database {:classname "org.h2.Driver"
                                :subprotocol "h2"
                                :user "sa"
                                :password ""
                                :subname (str "mem:" db-name)
                                :unsafe true}
                     }
        test-config (conf/post-process-config :standalone test-config)
        ]    
    (conf/init-config test-config)
    (entities/init)
    ))

(defn create-db-schema []
  (migrations/do-migration :forward))

(defn drop-db-schema []
  (migrations/do-migration :rollback))


