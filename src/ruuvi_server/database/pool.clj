(ns ruuvi-server.database.pool
  (:import com.jolbox.bonecp.BoneCPDataSource)
  (:use lobos.connectivity)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn- bonecp-connection-pool [dbh]
  (info "Create bonecp connection pool")
  (let [jdbc-url (str "jdbc:" (:subprotocol dbh) ":" (:subname dbh))
        connection-pool (doto (BoneCPDataSource.)
                          (.setDriverClass (dbh :classname))
                          (.setJdbcUrl jdbc-url)
                          (.setPartitionCount 3)
                          (.setMaxConnectionsPerPartition 26)
                          (.setStatementsCacheSize 100)
                          (.setStatementReleaseHelperThreads 3)
                          (.setReleaseHelperThreads 3)
                          )]
    (when (:user dbh) (.setUsername connection-pool (:user dbh)))
    (when (:password dbh) (.setPassword connection-pool (:password dbh)))
    connection-pool)
  )

(defn create-connection-pool [dbh]
  (bonecp-connection-pool dbh))
