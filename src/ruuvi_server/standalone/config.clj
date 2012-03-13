(ns ruuvi-server.standalone.config
  (:use korma.db)
  (:use ruuvi-server.database)
  (:use ruuvi-server.models.entities)
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
  
  (defdb db (postgres (create-connection-pool *database-config*)))
  (init-entities)
  )