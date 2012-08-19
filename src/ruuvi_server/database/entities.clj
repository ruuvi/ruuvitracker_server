(ns ruuvi-server.database.entities
  (:use korma.db)
  (:use korma.core)
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.database.pool :as pool]
            [ruuvi-server.configuration :as conf])
  (:import org.joda.time.DateTime)
  )

(declare tracker)
(declare event-extension-type)
(declare event-extension-value)
(declare event-session)
(declare event)
(declare event-annotation)

(defn init []
  (in-ns 'ruuvi-server.database.entities)
  (info "Initializing database connection")
  (let [database-conf (:database (conf/get-config))
        conn-pool (pool/create-connection-pool database-conf)
      pooled-conn {:pool {:datasource conn-pool}
                   :options (korma.config/extract-options {})}]
    (default-connection pooled-conn)
    )
  
  (info "Mapping entities")
  
  (defentity tracker
    (table :trackers)
    (pk :id)
    (entity-fields :id :tracker_code :name :latest_activity :shared_secret)
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
    (entity-fields :latitude :longitude)
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
  
  )