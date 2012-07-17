(ns ruuvi-server.database.pool
  (:import [org.apache.tomcat.jdbc.pool DataSource])
  (:use lobos.connectivity)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn create-connection-pool [dbh]
  (info "Create connection pool")
  (let [connection-pool (doto (DataSource.)
                          (.setDriverClassName (dbh :classname))
                          (.setUrl (str "jdbc:" (:subprotocol dbh) ":" (:subname dbh)))
                          (.setUsername (:user dbh))
                          (.setPassword (:password dbh))
                          )]
    (merge dbh {:datasource connection-pool}
           )))
