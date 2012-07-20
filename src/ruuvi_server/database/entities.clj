(ns ruuvi-server.database.entities
  (:use korma.db)
  (:use korma.core)
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:require [ruuvi-server.util :as util])
  (:import org.joda.time.DateTime)
  (:require [ruuvi-server.configuration :as conf])
  (:require [ruuvi-server.database.pool :as pool])
  )

(declare tracker)
(declare event-extension-type)
(declare event-extension-value)
(declare event)

(defdb db (:database conf/*config*))
(info "Mapping entities")
  
(defentity tracker
  (table :trackers)
  (pk :id)
  (entity-fields :id :tracker_code :name :latest_activity :shared_secret)
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
  (has-many event-location {:fk :event_id})
  (has-many event-extension-value {:fk :event_id})
  )


