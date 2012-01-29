(ns ruuvi-server.lobos.migration
  (:refer-clojure :exclude [alter drop
                            bigint boolean char double float time complement])
  (:use (lobos [migration :only [defmigration]] connectivity core schema))
  (:use ruuvi-server.database)
  (:use ruuvi-server.lobos.helpers)
  )

(defmigration add-tracker-table
  (up [] (create
          (table :tracker
               (varchar :tracker_identifier 100 :unique)
               (varchar :name 100)
               (timestamp :latest_activity)
               (varchar :shared_secret 64)
               )))
  (down [] (drop (table :tracker))))

;; TODO this doesn't work. Doesn't do anything.
(defn -main []
  (println "Migration start")
  (migrate add-tracker-table)
  (println "Migration end")
  )