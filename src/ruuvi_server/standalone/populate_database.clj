(ns ruuvi-server.standalone.populate-database
  (:use ruuvi-server.standalone.config)
  (:use ruuvi-server.database.load-initial-data)
  )

(defn -main [] 
  (init-config)
  (create-test-trackers)
  )
