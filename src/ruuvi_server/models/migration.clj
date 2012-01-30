(ns ruuvi-server.models.migration
  (:require [clojure.java.jdbc :as sql])
  )

(defn migrate [direction database-config]
  (if (= direction "up")
    (sql/with-connection database-config
      (sql/create-table :trackers
                        [:id :serial "primary key"]
                        [:tracker_identifier :varchar "not null"]
                        [:name :varchar]
                        [:latest_activity :timestamp]
                        [:shared_secret :varchar]
                        [:created_at :timestamp "not null" "default current_timestamp"])     
      
      (sql/create-table :events
                        [:id :serial "primary key"]
                        [:tracker_id :integer "not null" "references trackers(id) on delete cascade"]
                        [:latest_activity :timestamp]
                        [:created_at :timestamp "not null" "default current_timestamp"])
      
      (sql/create-table :event_locations
                        [:id :serial "primary key"]
                        [:event_id :integer "not null" "references events(id) on delete cascade"]
                        [:latitude :varchar "not null"]
                        [:longitude :varchar "not null"]
                        [:accuracy :varchar]
                        [:satellite_count :integer]
                        [:altitude :decimal]                        
                        [:created_at :timestamp "not null" "default current_timestamp"])

      (sql/create-table :event_annotations 
                        [:id :serial "primary key"]
                        [:annotation :text]
                        [:event_id :integer "not null" "references events(id) on delete cascade"]
                        [:created_at :timestamp "not null" "default current_timestamp"])

      (sql/create-table :event_extension_types
                        [:id :serial "primary key"]
                        [:name :varchar "not null"]
                        [:description :varchar "not null"]
                        [:created_at :timestamp "not null" "default current_timestamp"])

      (sql/create-table :event_extension_values
                        [:id :serial "primary key"]
                        [:event_id :integer "not null" "references events(id) on delete cascade"]
                        [:extension_type_id :integer "not null" "references event_extension_types(id) on delete cascade"]
                        [:value :varchar]
                        )
      )
    (sql/with-connection database-config
      (sql/drop-table :event_extension_values)
      (sql/drop-table :event_extension_types)
      (sql/drop-table :event_annotations)
      (sql/drop-table :event_locations)
      (sql/drop-table :events)
      (sql/drop-table :trackers)
      ) 
 ))