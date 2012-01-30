(ns ruuvi-server.heroku.heroku-migration
  (:use ruuvi-server.heroku.heroku-config)
  (:use ruuvi-server.database)
  (:use ruuvi-server.models.migration)
  )

(defn -main [direction]
  (migrate direction (create-connection-pool database-config))
)

