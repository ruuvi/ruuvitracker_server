(ns ruuvi-server.api
  "REST API structure"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.tracker-api :as tracker-api]
            [ruuvi-server.client-api :as client-api]
            [ruuvi-server.user-api :as user-api]
            [ruuvi-server.websocket-api :as websocket-api]
            [ruuvi-server.middleware :as middleware]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.session :as session]
            )
  (:use [compojure.core :only (defroutes GET OPTIONS PUT POST DELETE ANY context)]
        [ring.middleware.json 
         :only (wrap-json-params wrap-json-response wrap-json-body)]
        [ring.middleware.keyword-params :only (wrap-keyword-params)]
        [ring.middleware.params :only (wrap-params)]
        [clojure.tools.logging :only (debug info warn error)])
  )

(def url-prefix "/v1-dev")

(defn- success-handler
  "Returns success answer without content"
  [request]
  {:status 200
   :headers {"Content-Type" "application/json;charset=UTF-8"}})

(def id-list-regex
  "Matches commaseparated list of integers"
  #"([0-9]+,?)+")

(defroutes api-routes-internal
  ;; Client-API
  (GET "/ping" [] client-api/ping)

  (GET ["/sessions/:ids/events" :ids id-list-regex] [ids]
       #(client-api/fetch-events
         (merge % {:event_session_ids ids})))

  (GET ["/sessions/:ids/events/latest" :ids id-list-regex] [ids]
       #(client-api/fetch-events
         (merge % {:event_session_ids ids})))
  
  (GET ["/sessions/:ids" :ids id-list-regex] [ids]
       #(client-api/fetch-session (merge % {:event_session_ids ids})))

  (GET ["/sessions/:ids/events" :ids id-list-regex] [ids]
       #(client-api/fetch-tracker % ids))
  
  (GET "/trackers" [] client-api/fetch-trackers)
  
  (GET ["/events/:ids" :ids id-list-regex] [ids]
       #(client-api/fetch-event % ids))

  (GET "/events" [] client-api/fetch-events)

  ;;; Users & Groups API
  (GET "/users" []
       user-api/fetch-users)
  (GET ["/users/:ids" :ids id-list-regex] [ids]
       #(user-api/fetch-users % (util/string-to-ids ids)))
  (GET ["/users/:ids/groups" :ids id-list-regex] [ids]
       user-api/fetch-user-groups)
  (POST ["/users/:ids/groups" :ids id-list-regex] [ids]
        user-api/add-user-group)
  (POST "/users" []
        user-api/create-user)
  (DELETE ["/users/:user-ids/groups/:group-ids" 
          :user-ids id-list-regex :group-ids id-list-regex]
          [user-ids group-ids]
        #(user-api/remove-user-group %))
  
  (GET ["/groups/:ids/users" :ids id-list-regex] [ids]
       user-api/fetch-group-users)
  (GET ["/groups/:ids/trackers" :ids id-list-regex] [ids]
       user-api/fetch-group-trackers)
  (GET ["/groups/:ids" :ids id-list-regex] [ids]
       #(user-api/fetch-groups % (util/string-to-ids ids)))
  (GET "/groups" []
       user-api/fetch-groups)

  (POST "/groups" []
        user-api/create-group)
  (DELETE ["/groups/:ids" :ids id-list-regex] [ids]
          user-api/remove-groups)

  (GET ["/trackers/:ids/users" :ids id-list-regex] [ids]
       user-api/fetch-tracker-groups)
  (POST ["/trackers/:ids/groups" :ids id-list-regex] [ids]
        user-api/add-tracker-group)
  (DELETE ["/trackers/:user-ids/groups/:group-ids" 
          :tracker-ids id-list-regex :group-ids id-list-regex]
          [user-ids group-ids]
        user-api/remove-tracker-group)
  (POST "/trackers" [] client-api/create-tracker)

  ;; TODO DEPRECATED remove
  (PUT "/trackers" [] client-api/create-tracker)
  (GET ["/trackers/:ids" :ids id-list-regex] [ids]
       #(client-api/fetch-tracker % ids))
  (GET ["/trackers/:ids/events" :ids id-list-regex] [ids]
       #(client-api/fetch-events (merge % {:tracker_ids ids})))
  (GET ["/trackers/:ids/events/:order" 
        :ids id-list-regex 
        :order #"latest|latestStored"] [ids order]
       (fn [request]
         (let [order-by (cond (= order "latest") :latest-event-time
                              (= order "latestStored") :latest-store-time)]
           (client-api/fetch-events (merge request {:tracker_ids ids :order-by order-by})))) )
  
  (GET ["/trackers/:ids/sessions" :ids id-list-regex] [ids]
       #(client-api/fetch-session (merge % {:tracker_ids ids})))

  (GET "/auths" [] user-api/check-auth-cookie)
  (POST "/auths" [] user-api/authenticate)
  (DELETE "/auths" [] user-api/logout)

  ;; Websockets API
  (GET "/websocket" [] (websocket-api/websocket-api-handler))
  ;; Tracker-API
  (POST "/events" [] tracker-api/handle-create-event)
  ;; OPTIONS method for every url to enable CORS headers
  (OPTIONS "*" [] success-handler)
  ;; Fallback for everything not caught above
  (ANY "*" [] #(util/json-response % {:error "Unsupported operation"} 404))
)

(def ^{:private true} request-counter
  "Counts incoming requests."
  (atom 0))

(def api-routes-with-wrappers 
           (-> api-routes-internal
               (middleware/wrap-authentication)
               (wrap-keyword-params)
               (session/wrap-session {:cookie-name "ruuvitracker_web"
                                      :cookie-attrs {:max-age 3600}})
               (wrap-params)
               (wrap-json-body {:keywords? true})
               (wrap-json-params)
               (middleware/wrap-json-response)
               (middleware/wrap-strip-trailing-slash)
               (middleware/wrap-cors-headers)
               (middleware/wrap-exception-logging)
               (middleware/wrap-error-handling)
               (middleware/wrap-request-logger request-counter)))

(defroutes api-routes
  (context url-prefix [] api-routes-with-wrappers)
  ;; fallback
  (fn [request] (util/json-error-response request "Resource not found" 404)))

