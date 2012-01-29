(ns ruuvi-server.database
  (:import [org.apache.tomcat.jdbc.pool DataSource])
  (:use lobos.connectivity))

;; TODO read from DATABASE_URL env
(def dbconfig
     {:classname "org.postgresql.Driver"
      :subprotocol "postgresql"
      :user "ruuvi"
      :password "ruuvi"
      :subname "//localhost/ruuvi_server"})

(defn database-handle [dbh]
  (let [connection-pool (doto (DataSource.)
                          (.setDriverClassName (dbh :classname))
                          (.setUrl (str "jdbc:" (:subprotocol dbh) ":" (:subname dbh)))
                          (.setUsername (:user dbh))
                          (.setPassword (:password dbh))
                          )]
    (merge dbh {:datasource connection-pool}
           )))

(open-global (database-handle (merge dbconfig {:auto-commit true})))
