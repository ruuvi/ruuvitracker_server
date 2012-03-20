(ns ruuvi-server.standalone.config
  (:use korma.db)
  (:use ruuvi-server.database.pool)
  (:use ruuvi-server.database.entities)
  (:require [korma.config :as conf])
  )

(defn init-config []
  (in-ns 'ruuvi-server.standalone.config)
  (def *database-config*
    {:classname "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user "ruuvi"
     :password "ruuvi"
     :subname "//localhost/ruuvi_server"})
  (def *server-port* 8080)

  (defn- db-with-connection-pool [spec]
    (let [engine-conf (postgres spec)]
      (defonce db {:pool (create-connection-pool spec)
                   :options (conf/extract-options engine-conf)})
      (default-connection db)
      )
    )


  (defdb db (postgres *database-config*))
  ;; setup korma to use connection pool of my choosing
  ;;(db-with-connection-pool *database-config*)
  (init-entities)
)