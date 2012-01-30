(ns ruuvi-server.database
  (:import [org.apache.tomcat.jdbc.pool DataSource])
  (:use lobos.connectivity)
  (:use ruuvi-server.database-config)
  )

(defn create-connection-pool [dbh]
  (let [connection-pool (doto (DataSource.)
                          (.setDriverClassName (dbh :classname))
                          (.setUrl (str "jdbc:" (:subprotocol dbh) ":" (:subname dbh)))
                          (.setUsername (:user dbh))
                          (.setPassword (:password dbh))
                          )]
    (merge dbh {:datasource connection-pool}
           )))
