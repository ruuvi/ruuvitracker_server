(ns ruuvi-server.message
  "Functions to convert database entities to protocol messages."
  (:require [ruuvi-server.util :as util])
  (:use [clojure.tools.logging :only (debug info warn error)]))

(defn- select-extension-data
  "Convert extension_data to name value pairs. Skip extension data if name is missing."
  [extension-data]
  
  (filter identity
          (map (fn [data]
                 (when-let [name (:name data)]
                   {name (:value data)}))
               extension-data)))
  
(defn- select-location-data [location-data]
  (when location-data
    (let [selected-data (select-keys location-data
                                     [:longitude :latitude :altitude
                                      :heading :satellite_count :speed
                                      :horizontal_accuracy :vertical_accuracy])
          renamed-data (util/modify-map selected-data
                                        {:horizontal_accuracy :accuracy}
                                        {})]
      (util/remove-nil-values renamed-data)
      )))

(defn select-event-data [event-data]
  (let [selected-data (select-keys event-data
                                   [:id :event_time :tracker_id
                                    :event_session_id :created_on])
        renamed-data (util/modify-map selected-data
                                      {:created_on :store_time}
                                      {})
        renamed-data (util/stringify-id-fields renamed-data)
        location-data (select-location-data
                       (get (event-data :event_locations)
                            0))
        extension-data (select-extension-data
                        (event-data :event_extension_values))]

    (util/remove-nil-values (merge renamed-data
                                   {:location location-data
                                    :extension_values extension-data}))
    ))

(defn select-events-data [data-map]
  {:events
   (map select-event-data (data-map :events))}
  )

(defn select-event-session-data [session-data]
  (let [selected-data (select-keys session-data
                                   [:id :tracker_id :session_code
                                    :first_event_time :latest_event_time])
        selected-data (util/stringify-id-fields selected-data)]
    (util/remove-nil-values selected-data)))
                                    
(defn select-event-sessions-data [data-map]
  {:sessions
   (map select-event-session-data (data-map :event_sessions))})

(defn select-tracker-data [data-map]
  (let [selected (select-keys data-map [:id :tracker_code :name :description
                                        :latest_activity :created_on])
        renamed (util/modify-map selected nil {:id str})]
    (util/remove-nil-values renamed)))
  
(defn select-trackers-data [data-map]
  {:trackers (map select-tracker-data (data-map :trackers))})
