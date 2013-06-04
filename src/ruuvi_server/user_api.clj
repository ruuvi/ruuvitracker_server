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
            [ring.util.response :as response]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [clojure.set :only (rename-keys)]
        )
  )
;; TODO handle authorization here
;; TODO move request/:params parsing to somewhere else?
;; TODO move {:users [] } wrapper to rest-api?
(defn db-conn []
  (get-in (conf/get-config) [:database]))

;; Users
(defn fetch-users [req user-ids]
  (let [x (dao/get-users (db-conn) user-ids)
        result {:users (vec x)}]
    {:body result}
    ))

(defn fetch-user-groups [req]
  {:body {:not-implemented :yet} :status 501} )

(defn- auth-data [user]
  {:user-id (:id user)}
)

(defn create-user [request]
  (let [user (get-in request [:params :user])
        ;; TODO bcrypt
        user (rename-keys user {:password :password_hash}) 
        created-user (jdbc/db-transaction [t-conn (db-conn)]
                                          (dao/create-user! (db-conn) user))]
    (info "User" (:username user) "registered.")
    {:body {:message "User created"
            :user created-user}
     :session (auth-data user) }
))
  

(defn add-user-group [req]
  (let [user (get-in req [:params :user])
        group (get-in req [:params :group])]
      (jdbc/db-transaction [t-conn (db-conn)]
                           (dao/add-user-to-group! t-conn user group "owner")
       )))

(defn remove-user-group [req]
  {:body {:not-implemented :yet} :status 501})

;; Groups
(defn fetch-groups [req group-ids]
  (let [x (dao/get-groups (db-conn) group-ids)
        result {:groups (vec x)}]
    {:body result}
    ))

(defn fetch-group-users [req]
  {:body {:not-implemented :yet} :status 501})

(defn fetch-group-trackers [req group-ids]
  (let [x (dao/get-group-trackers (db-conn) group-ids)
        result {:trackers (vec x)}]
    {:body result}
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
  {:body {:not-implemented :yet} :status 501})

(defn add-tracker-group [req]
  (let [tracker (get-in req [:params :tracker])
        group (get-in req [:params :group])]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/add-tracker-to-group! (db-conn) tracker group))))

(defn remove-tracker-group [req]
  {:body {:not-implemented :yet} :status 501})

(defn- public-user [user]
  (dissoc user :password_hash :email))

;; consider also access token??
(defn- valid-session? [req]
  (let [user-id (-> req :session :user-id)]
    (not (nil? user-id))))

(defn- password-matches? [user password]
  ;; TODO bcrypt
  (info user password)
  (= (:password_hash user) password))

;; Authentication
(defn authenticate 
  "Authenticates user using HTTP Basic auth.
Add a new auth cookie."
  [req]
  (let [username (-> req :params :user :username)
        password (-> req :params :user :password)
        session (-> req :session)
        user (dao/get-user-by-username (db-conn) username)]
    (cond (not user)
          {:body {:authenticated false
                  :error "Unknown user or wrong password"
                  :debug "user"}
           :status 401}
          (password-matches? user password)
          {:body {:authenticated true
                  :message (str "login " (:username user)) 
                  :user (public-user user)}
           :session (auth-data user)
           :status 200}
          :default
          {:body {:authenticated false
                  :error "Unknown user or wrong password"
                  :debug "passwd"}
           :status 401})))

(defn logout
  "Destroys user session"
  [req]
  {:body {:authenticated false}
   :session {}} )

(defn check-auth-cookie
  "Checks if authentication cookie is valid."
  [req]
  (if (valid-session? req)
    {:body {:authenticated true}}
    {:body {:authenticated false}} ))

