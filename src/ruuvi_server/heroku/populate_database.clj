(ns ruuvi-server.heroku.populate-database
  (:use ruuvi-server.heroku.config)
  (:use ruuvi-server.load-initial-data)
  )

(defn -main [] 
  (create-test-trackers)
  )