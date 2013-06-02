(ns ruuvi-server.database.user-dao
  (:require [clojure.java.jdbc :as sql]
            [ruuvi-server.cache :as cache]
            [clojure.string :as string]
            )
  (:use [ruuvi-server.database.entities :only ()]
        [clj-time.core :only (date-time now)]
        [clj-time.coerce :only (to-timestamp)]
        [clojure.tools.logging :only (debug info warn error)]
        )
)


(defn 
  ;;^{:private true} 
  in [field values]
  (let [q-marks (string/join "," (take (count values) (repeat "?"))) ]
    (vec (concat [(str (name field) " in (" q-marks ")")] values))
    ))

(defn get-users 
  [db user-ids]
  (let [query (if user-ids 
                (in "select u.* from users u where u.id"
                    (vec user-ids))
                 ["select u.* from users u"])]
    (sql/query
      db
      query
      :row-fn #(dissoc % :password_hash :email))))

(defn get-groups-for-user "Fetch all groups where user is a member."
  [db user-id] 
  (let [query (in "select g.* from groups g 
join users_groups ug on (g.id = ug.group_id) 
where ug.user_id" (vec user-id))]
    (sql/query
     db
     query
     :row-fn #(dissoc % :password_hash))))


(defn get-group-users "Fetch all users that belong to given group."
  [db group-ids] 
  (let [query (in "select t.* from users u 
join users_groups tg on (u.id = ug.user_id) 
where ug.group_id" group-ids)]
    (sql/query
      db
      query
      :row-fn #(dissoc % :password_hash))))


(defn get-group-trackers "Fetch all trackers that belong to group." 
  [db group-ids] 
  (let [query (in "select t.* from trackers t 
join trackers_groups tg on (t.id = tg.tracker_id) 
where tg.group_id"
                  group-ids)]
    (sql/query
      db
      query
      :row-fn #(dissoc % :password :shared_secret))))


(defn get-groups [db group-ids] 
  (let [query (if group-ids 
                (in "select g.* from groups g where g.id"
                    (vec group-ids))
                 ["select g.* from groups g"])]
    (sql/query
      db
      query)))


;; transactions and authorization are handled in user_api
;; simple CRUD methods are here
(defn create-user!
  [db user] 
  (let [db-user (select-keys user [:username :email :name :password_hash])
        new-user (sql/insert! db :users db-user)]
    (dissoc (first new-user) :password_hash)
    ))

(defn create-group! 
  [db group] 
  (let [db-group (select-keys group [:name])]
    (first (sql/insert! db :groups db-group))
    ))

(defn remove-group! 
  [group-ids]
  (sql/delete-rows :groups (in :id group-ids)))

(defn add-tracker-to-group!
  [db tracker group] 
  (let [user-group {:tracker_id (:id tracker) 
                    :group_id (:id group)}]
    (first (sql/insert! db :trackers_groups user-group))))

(defn add-user-to-group! 
  [db user group role] 
  (let [user-group {:user_id (:id user) 
                    :group_id (:id group)
                    :role role}]
    (first (sql/insert! db :users_groups user-group) )))

(defn remove-trackers-from-group! [user] )

(defn remove-users-from-group! [] )

