(ns ruuvi-server.database.user-dao
  (:require [clojure.java.jdbc :as sql]
            [ruuvi-server.cache :as cache]
            )
  (:use [ruuvi-server.database.entities :only ()]
        [clj-time.core :only (date-time now)]
        [clj-time.coerce :only (to-timestamp)]
        [clojure.tools.logging :only (debug info warn error)]
        )
)

(defn get-groups-for-user "Fetch all groups where user is a member."
  [] )

(defn get-group-users "Fetch all users that belong to given group."
  [] )

(defn get-group-trackers "Fetch all trackers that belong to group." 
  [] )

(defn get-groups [ids] )


(defn create-user! [] )

(defn create-group! [] )

(defn remove-group! [] )

(defn add-tracker-to-group! [] )

(defn add-user-to-group! [] )

(defn remove-trackers-from-group! [] )

(defn remove-users-from-group! [] )

