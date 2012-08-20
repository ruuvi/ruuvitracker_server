(ns ruuvi-server.database.test.migrations
  (:use midje.sweet)
  (:require [ruuvi-server.database.test.database-setup :as database-setup]
            [ruuvi-server.database.event-dao :as event-dao]
            ))

(defn- schema-exists? []
  (try
    (event-dao/get-trackers [1])
    true
    (catch java.sql.SQLException e false)))

(fact "Database connection setup works"
      (database-setup/setup-db-connection "dummy_internal")
      (schema-exists?) => false)

(fact "Database migrations apply"
      (database-setup/create-db-schema)
      (schema-exists?) => true)

(fact "Database migrations can be applied several times"
      (database-setup/create-db-schema)
      (database-setup/create-db-schema)
      (schema-exists?) => true)

(fact "Database migrations rollback"
      (database-setup/drop-db-schema)
      (schema-exists?) => false)

(fact "Database migrations can be rollbacked several times"
      (database-setup/drop-db-schema)
      (database-setup/drop-db-schema)
      (schema-exists?) => false)

(fact "Database migrations can be re-applied after rollback"
      (database-setup/create-db-schema)
      (schema-exists?) => true)

(fact "Database migrations can be rollbacked after re-applying"
      (database-setup/drop-db-schema)
      (schema-exists?) => false)
