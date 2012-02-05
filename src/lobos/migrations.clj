(ns lobos.migrations
  (:refer-clojure :exclude [alter drop
                            bigint boolean char double float time complement])
  (:use (lobos [migration :only [defmigration]] connectivity core schema))
  (:use lobos.helpers)

  (:use ruuvi-server.standalone.config)
  (:use ruuvi-server.database)
  )

(defmigration add-trackers-table
  (up []
      (create
              (table-entity :trackers
                            (varchar :tracker_identifier 256 :not-null :unique)
                            (varchar :name 256)
                            (timestamp :latest_activity)
                            (varchar :shared_secret 64)
                            ))      
      (create (index :trackers :ix_trackers_name [:name]))
     )
  (down [] (drop (table :trackers))))

(defmigration add-events-table
  (up []
      (create
       (table-entity :events
                     (refer-to :trackers)
                     (timestamp :event_time :not-null)
                     ))
      (create (index :events :ix_events_event_time [:event_time]))
      )
  
  (down [] (drop (table :events))))

(defmigration add-event-locations-table
  (up [] (create
          (table-entity :event_locations
                        (refer-to :events)
                        (varchar :latitude 20 :not-null)
                        (varchar :longitude 20 :not-null)
                        (varchar :accuracy 20)
                        (integer :satellite_count)
                        (decimal :altitude))))
  (down [] (drop (table :event_locations))))

(defmigration add-event-annotations-table
  (up [] (create
          (table-entity :event_annotations
                        (refer-to :events)
                        (varchar :annotation 256))))
  (down [] (drop (table :event_annotations))))

(defmigration add-event-extension-types-table
  (up []
      (create
       (table-entity :event_extension_types
                     (varchar :name 256 :not-null)
                     (varchar :description 256)))
      (create (index :event_extension_types [:name]))
      )
  (down [] (drop (table :event_extension_types))))

(defmigration add-event-extension-values-table
  (up [] (create
          (table-entity :event_extension_values
                        (refer-to :events)
                        (refer-to :event_extension_types)
                        (varchar :value 256))))
  (down [] (drop (table :event_extension_values))))

(defn do-migration [direction db-config]
  (println "Execute" (name direction))
  (open-global (create-connection-pool *database-config*))
  (if (= :rollback direction)
    (rollback)
    (migrate)
    )
  (println "Done")   
  )
