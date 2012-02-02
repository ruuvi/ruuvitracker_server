(ns ruuvi-server.heroku.migration
  (:use ruuvi-server.heroku.config)
  (:use ruuvi-server.database)
  )

(defn -main []
  (open-global (create-connection-pool *database-config*))
  (migrate)
)

