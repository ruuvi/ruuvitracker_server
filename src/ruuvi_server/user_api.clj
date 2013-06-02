(ns ruuvi-server.user-api
  "User API implementation"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.user-dao :as dao]
            [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            [ruuvi-server.common :as common]
            [ruuvi-server.message :as message]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [clojure.set :only (rename-keys)]
        )
  )
;; TODO handle authorization here
;; TODO move request/:params parsing to somewhere else?
;; TODO move json-response to rest api
;; TODO move {:users [] } wrapper to rest-api?
(defn db-conn []
  (get-in (conf/get-config) [:database]))

;; Users
(defn fetch-users [req user-ids]
  (let [x (dao/get-users (db-conn) user-ids)
        result {:users (vec x)}]
    (util/json-response req result)
    ))

(defn fetch-user-groups [req]
  (util/json-response req {:not-implemented :yet}) )

(defn create-user [request]
  (let [user (get-in request [:params :user])
        user (rename-keys user {:password :password_hash}) ]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/create-user! (db-conn) user))))
  

(defn add-user-group [req]
  (let [user (get-in req [:params :user])
        group (get-in req [:params :group])]
      (jdbc/db-transaction [t-conn (db-conn)]
                           (dao/add-user-to-group! t-conn user group "owner")
       )))

(defn remove-user-group [req]
  (util/json-response req {:not-implemented :yet}))

;; Groups
(defn fetch-groups [req group-ids]
  (let [x (dao/get-groups (db-conn) group-ids)
        result {:groups (vec x)}]
    (util/json-response req result)
    ))

(defn fetch-group-users [req]
  (util/json-response req {:not-implemented :yet}))

(defn fetch-group-trackers [req group-ids]
  (let [x (dao/get-group-trackers (db-conn) group-ids)
        result {:trackers (vec x)}]
    (util/json-response req result)
    ))

(defn create-group [request]
  (let [group (get-in request [:params :group])]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/create-group! (db-conn) group))))

(defn remove-groups [req group-ids]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/remove-group! (db-conn) group-ids)))

;; Trackers
(defn fetch-tracker-groups [req]
  (util/json-response req {:not-implemented :yet}))

(defn add-tracker-group [req]
  (let [tracker (get-in req [:params :tracker])
        group (get-in req [:params :group])]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/add-tracker-to-group! (db-conn) tracker group))))

(defn remove-tracker-group [req]
  (util/json-response req {:not-implemented :yet})
)

;; Authentication
(defn authenticate 
  "Authenticates user using HTTP Basic auth.
Add a new auth cookie."
  [req]
  (util/json-response req {:not-implemented :yet})
)

(defn check-auth-cookie
  "Checks if authentication cookie is valid."
  [req]
  (util/json-response req {:not-implemented :yet})
)
