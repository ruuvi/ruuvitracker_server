(ns ruuvi-server.database.pool
  (:import [org.apache.tomcat.jdbc.pool DataSource])
  (:use lobos.connectivity)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn create-connection-pool [dbh]
  (info "Create connection pool")
  (let [connection-pool (doto (DataSource.)
                          (.setDriverClassName (dbh :classname))
                          (.setUrl (str "jdbc:" (:subprotocol dbh) ":" (:subname dbh))))]
    (when (:user dbh) (.setUserName connection-pool (:user dbh)))
    (when (:user dbh) (.setPassword connection-pool (:password dbh)))    
    (merge dbh {:datasource connection-pool}
           )))
