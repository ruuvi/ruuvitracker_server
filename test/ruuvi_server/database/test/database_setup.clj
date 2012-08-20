(ns ruuvi-server.database.test.database-setup
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
                  }]
    
    (conf/init-config test-config)
    (entities/init)
    ))

(defn create-db-schema []
  (migrations/do-migration :forward))

(defn drop-db-schema []
  (migrations/do-migration :rollback))

(fact "Database connection setup works"
      (setup-db-connection "dummy_internal") => truthy)

(fact "Database migrations apply"
      (create-db-schema) => nil)

(fact "Database migrations can be applied several times"
      (create-db-schema)
      (create-db-schema) => nil)

(fact "Database migrations rollback"
      (create-db-schema) => nil)

(fact "Database migrations can be re-applied after rollback"
      (drop-db-schema) => nil)

(fact "Database migrations can be rollbacked after re-applying"
      (drop-db-schema) => nil)
