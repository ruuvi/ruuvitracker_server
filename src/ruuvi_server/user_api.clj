(ns ruuvi-server.user-api
  "User API implementation"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.parse :as parse]
            [ruuvi-server.database.user-dao :as db]
            [clojure.string :as string]
            [ruuvi-server.common :as common]
            [ruuvi-server.message :as message]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [clojure.set :only (rename-keys)]
        )
  )

;; Users
(defn fetch-users [req]
  ;;(util/json-response req {:not-implemented :yet})
  {:body {:not-implemented :yet}}
)

(defn fetch-user-groups [req]
  (util/json-response req {:not-implemented :yet})
  )

(defn create-user [req]
  (util/json-response req {:not-implemented :yet})
)

(defn add-user-group [req]
  (util/json-response req {:not-implemented :yet})
)

(defn remove-user-group [req]
  (util/json-response req {:not-implemented :yet})
)

;; Groups
(defn fetch-groups [req]
  (util/json-response req {:not-implemented :yet})
)

(defn fetch-group-users [req]
  (util/json-response req {:not-implemented :yet})
)

(defn fetch-group-trackers [req]
  (util/json-response req {:not-implemented :yet})
)

(defn create-group [req]
  (util/json-response req {:not-implemented :yet})
)

(defn remove-groups [req]
  (util/json-response req {:not-implemented :yet})
)

;; Trackers
(defn fetch-tracker-groups [req]
  (util/json-response req {:not-implemented :yet})
)

(defn add-tracker-group [req]
  (util/json-response req {:not-implemented :yet})
)

(defn remove-tracker-group [req]
  (util/json-response req {:not-implemented :yet})
)

;; Authentication
(defn authenticate 
  "Authenticates user using HTTP Basic auth.
Add a new auth cookie."
  [req]
)

(defn check-auth-cookie
  "Checks if authentication cookie is valid."
  [req]
)
