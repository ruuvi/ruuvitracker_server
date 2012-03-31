(ns ruuvi-server.heroku.populate-database
  (:use ruuvi-server.heroku.config)
  (:use ruuvi-server.database.load-initial-data)
  )

(defn -main [] 
  (init-config)
  (create-test-trackers)
  )
