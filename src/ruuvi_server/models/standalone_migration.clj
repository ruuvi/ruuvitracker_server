(ns ruuvi-server.models.standalone-migration
  (:use ruuvi-server.database-config)
  (:use ruuvi-server.database)
  (:use ruuvi-server.models.migration)
  )

(defn -main [direction]
  (migrate direction (create-connection-pool database-config))
)

