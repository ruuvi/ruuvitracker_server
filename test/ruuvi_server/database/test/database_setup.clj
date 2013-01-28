(ns ruuvi-server.database.test.database-setup
  "Functions to create database setup for unit-tests."
  (:require [ruuvi-server.configuration :as conf]
            [ruuvi-server.database.entities :as entities]
            [lobos.migrations :as migrations]
            )
  (:use midje.sweet)
  )


(defn setup-db-connection [db-name]
  (def partial-config {:database {:classname "org.h2.Driver"
                                  :subprotocol "h2"
                                  :user "sa"
                                  :password ""
                                  :subname (str "mem:" db-name)
                                  :unsafe true}
                       })
  (def test-config (conf/post-process-config :standalone partial-config))

  (conf/init-config test-config)
  (entities/init test-config)
  )

(defn create-db-schema []
  (migrations/do-migration test-config :forward))

(defn drop-db-schema []
  (migrations/do-migration test-config :rollback))
