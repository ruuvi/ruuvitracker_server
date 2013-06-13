(ns ruuvi-server.database.entities
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.database.pool :as pool]
            [ruuvi-server.configuration :as conf]
            )
  (:use [korma.db :only (default-connection)]
        [korma.core :only (defentity entity-fields table pk belongs-to has-many)]
        [clojure.tools.logging :only (debug info warn error)]
        )
  )

(declare tracker)
(declare event-extension-type)
(declare event-extension-value)
(declare event-session)
(declare event)
(declare event-annotation)

(defn init [config]
  (info "Initializing database connection")
  (let [database-conf (:database config)
        conn-pool (:datasource database-conf)
        pooled-conn {:pool {:datasource conn-pool}
                     :options (korma.config/extract-options {})}]
    (if conn-pool
      (default-connection pooled-conn)
      (default-connection database-conf))
    )
  )

(info "Mapping entities")
(defentity tracker
  (table :trackers)
  (pk :id)
  (entity-fields :id :tracker_code :name :latest_activity
                 :description :shared_secret :password)
  )

(defentity event-session
  (table :event_sessions)
  (pk :id)
  (entity-fields :id :session_code :first_event_time :latest_event_time)
  (belongs-to tracker {:fk :tracker_id})
  )

(defentity event-extension-type
  (table :event_extension_types)
  (pk :id)
  (entity-fields :name :description)
  )

(defentity event-extension-value
  (table :event_extension_values)
  (pk :id)
  (entity-fields :value)
  (belongs-to event-extension-type {:fk :event_extension_type_id})
  )

(defentity event-location
  (table :event_locations)
  (pk :id)
  (entity-fields :latitude :longitude :heading :altitude :speed
                 :horizontal_accuracy :vertical_accuracy :satellite_count)
  )

(defentity event
  (table :events)
  (pk :id)
  (entity-fields :event_time :created_on)
  (belongs-to tracker {:fk :tracker_id})
  (belongs-to event-session {:fk :event_session_id})
  (has-many event-location {:fk :event_id})
  (has-many event-extension-value {:fk :event_id})
  (has-many event-annotation {:fk :event_id})
  )

(defentity event-annotation
  (table :event_annotations)
  (pk :id)
  (entity-fields :event-annotation :created_on)
  )

  
