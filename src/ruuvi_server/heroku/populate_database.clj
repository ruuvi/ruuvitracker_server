(ns ruuvi-server.heroku.populate-database
  (:use ruuvi-server.database.load-initial-data)
  )

(defn -main [] 
  (create-test-trackers)
  )
