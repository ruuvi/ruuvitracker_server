(ns ruuvi-server.standalone.populate-database
  (:use ruuvi-server.database.load-initial-data)
  )

(defn -main []
  (create-test-trackers)
  )
