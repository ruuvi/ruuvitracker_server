(ns ruuvi-server.standalone.populate-database
  (:use ruuvi-server.standalone.config)
  (:use ruuvi-server.load-initial-data)
  )

(defn -main [] 
  (create-test-trackers)
  )