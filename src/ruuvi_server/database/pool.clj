(ns ruuvi-server.database.pool
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:import com.jolbox.bonecp.BoneCPDataSource
           com.jolbox.bonecp.BoneCPConfig)
  )

(defn- bonecp-connection-pool [dbh]
  (info "Create bonecp connection pool")
  (let [jdbc-url (str "jdbc:" (:subprotocol dbh) ":" (:subname dbh))
        ;; read config from /bonecp-config.xml
        bone-config (BoneCPConfig. "ruuviserver")
        connection-pool (doto (BoneCPDataSource. bone-config)
                          (.setDriverClass (dbh :classname))
                          (.setJdbcUrl jdbc-url))]
    (when (:user dbh) (.setUsername connection-pool (:user dbh)))
    (when (:password dbh) (.setPassword connection-pool (:password dbh)))
    connection-pool)
  )

(defn create-connection-pool [dbh]
  (bonecp-connection-pool dbh))
