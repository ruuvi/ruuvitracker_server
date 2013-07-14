(ns ruuvi-server.user-service
  "User API implementation"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.user-dao :as dao]
            [ruuvi-server.email-service :as email]
            [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            [ruuvi-server.common :as common]
            [ruuvi-server.message :as message]
            [ring.util.response :as response]
            [crypto.password.pbkdf2 :as password]
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

(defn- public-user [user]
  (dissoc user :password_hash :email))

;; Users
(defn fetch-users [req user-ids]
  (let [users (dao/get-users (db-conn) user-ids)
        result {:users (vec users)}]
    (util/response req result) ))

(defn fetch-user-groups [req]
  {:body {:not-implemented :yet} :status 501} )


(defn- session-auth-data [user]
  {:user-id (:id user)} )

(defn create-user [request]
  ;; TODO handle duplicate users
  (let [user (get-in request [:params :user])
        password (:password user)
        user (-> user 
                 (dissoc :password) 
                 (assoc :password_hash (password/encrypt password)))
        created-user (jdbc/db-transaction [t-conn (db-conn)]
                                          (dao/create-user! (db-conn) user))]
    (info "User" (:username user) "registered.")
    (let [email (-> user :email)
          name (or (-> user :name) email)
          username (-> user :username)]
      (email/send-registration! (conf/get-config) email name username))
    {:body {:authenticated true
            :message "User created"
            :user (public-user created-user)}
     :session (session-auth-data user) }
))
  

(defn add-user-group [req]
  (let [user (get-in req [:params :user])
        group (get-in req [:params :group])]
      (jdbc/db-transaction [t-conn (db-conn)]
                           (dao/add-user-to-group! t-conn user group "owner")
       )
      (util/response req {:result "ok"})))

(defn remove-user-group [req]
  {:body {:not-implemented :yet} :status 501})

;; Groups
(defn fetch-groups [req group-ids]
  (let [user-id (util/auth-user-id req)
        groups (dao/get-groups (db-conn) group-ids)
        result {:groups (vec groups)}]
    (util/response req result) ))

(defn fetch-visible-groups [req group-ids]
  (let [user-id (util/auth-user-id req)
        groups (dao/get-user-visible-groups (db-conn) user-id group-ids)
        result {:groups (vec groups)}]
    (util/response req result) ))

(defn fetch-group-users [req]
  {:body {:not-implemented :yet} :status 501})

(defn fetch-group-trackers [req group-ids]
  (let [x (dao/get-group-trackers (db-conn) group-ids)
        result {:trackers (vec x)}]
    (util/response req result) ))

;; TODO Location header
(defn create-group [request]
  (let [group (get-in request [:params :group])
        user-id (util/auth-user-id request)
        new-group (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/create-group! (db-conn) user-id group))]
    (util/response request {:result "ok" :group new-group} 201)))

(defn remove-groups [req group-ids]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/remove-group! (db-conn) group-ids))
    (util/response req {:result "ok"}))

;; Trackers
(defn fetch-tracker-groups [req]
  {:body {:not-implemented :yet} :status 501})

(defn add-tracker-group [req]
  (let [tracker (get-in req [:params :tracker])
        group (get-in req [:params :group])]
    (jdbc/db-transaction [t-conn (db-conn)]
                         (dao/add-tracker-to-group! (db-conn) tracker group))
    (util/response req {:result "ok"} 201)))

(defn remove-tracker-group [req]
  {:body {:not-implemented :yet} :status 501})

;; consider also access token??
(defn- valid-session? [req]
  (let [user-id (-> req :session :user-id)]
    (not (nil? user-id))))

(defn- password-matches? [user password]
  (password/check password (:password_hash user)))

(defn auth-data [user-id]
  (let [user (first (dao/get-users (db-conn) user-id))]
    (if user
      (let [groups (dao/get-user-visible-groups (db-conn) user-id)]
        {:user-id (:id user)
         :group-ids (map :id groups)})
      nil)))

(defn- auth-success [user]
  {:body {:authenticated true
          :message (str "login " (:username user)) 
          :user (public-user user)}
   :session (session-auth-data user)
   :status 200})

(def ^{:private true} dummy-password-hash 
  (password/encrypt "dummy-password-hash"))

(defn- failed-auth-delay 
  "Causes delay as long as successfull password check"
  [] 
  (password/check "dummy" dummy-password-hash))

(defn- auth-unknown-user [username]
  (failed-auth-delay)
  {:body {:authenticated false
          :error "Unknown user or wrong password"
          :debug "user"}
   :status 401})

(defn- auth-wrong-password [username]
  {:body {:authenticated false
          :error "Unknown user or wrong password"
          :debug "passwd"}
   :status 401})

;; Authentication
(defn authenticate 
  "Authenticates user using HTTP Basic auth.
Add a new auth cookie."
  [req]
  (let [username (-> req :params :user :username)
        password (-> req :params :user :password)
        session (-> req :session)
        user (dao/get-user-by-username (db-conn) username)]
    (cond (not user) (auth-unknown-user username)
          (password-matches? user password) (auth-success user)
          :default (auth-wrong-password username))))

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

