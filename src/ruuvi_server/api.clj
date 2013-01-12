(ns ruuvi-server.api
  "REST API structure"
  (:require [ruuvi-server.util :as util]
            [ruuvi-server.tracker-api :as tracker-api]
            [ruuvi-server.client-api :as client-api]
            [compojure.route :as route]
            [compojure.handler :as handler]
            )
  (:use [compojure.core :only (defroutes GET OPTIONS PUT POST context)]
        [ring.middleware.json :only (wrap-json-params)]
        [ring.middleware.keyword-params :only (wrap-keyword-params)]
        [ring.middleware.params :only (wrap-params)]
        [clojure.tools.logging :only (debug info warn error)])
  (:import com.fasterxml.jackson.core.JsonParseException)
  )

(def request-counter
  "Counts incoming requests."
  (atom 0))

(defn wrap-request-logger
  "Logs each incoming request"
  [app]
  (fn [request]
    (let [counter (swap! request-counter inc)
          request-method (:request-method request)
          uri (:uri request)
          query-params (:query-params request)
          start (System/currentTimeMillis)
          remote-addr (:remote-addr request)]
      (info (str "REQUEST:" counter)
            remote-addr request-method uri ":query-params" query-params)
      (let [response (app request)
            duration (- (System/currentTimeMillis) start)
            status (:status response)]
        (info (str "RESPONSE:" counter)
              remote-addr
              status
              duration "msec")
        response)
    )))

(defn wrap-error-handling
  "Catches exceptions and shows them as JSON errors"
  [handler]
  (fn [request]
    (try
      (or (handler request)
          (util/json-response request {"error" "resource not found"} 404))
      (catch JsonParseException e
        (util/json-response request {"error" "malformed json"} 400))
      (catch Exception e
        (util/json-response request {"error" "Internal server error"} 500)))))

(def url-prefix "/v1-dev")

(defn- success-handler
  "Returns success answer without content"
  [request]
  {:status 200
   :headers {"Content-Type" "application/json;charset=UTF-8"}})

(def id-list-regex
  "Matches commaseparated list of integers"
  #"([0-9]+,?)+")

;; TODO do some macro thing that creates automatically OPTIONS route
(defroutes api-routes-internal

  ;; Client-API
  (OPTIONS "/ping" []
           (-> #'success-handler))
  (GET "/ping" []
       (-> #'client-api/ping))

  (OPTIONS "/trackers" []
           (-> #'success-handler))
  (POST "/trackers" []
        (-> client-api/create-tracker))

  (PUT "/trackers" []
       (-> client-api/create-tracker))
  
  (OPTIONS ["/trackers/:ids" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/trackers/:ids" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           ))
  
  (OPTIONS ["/trackers/:ids/events" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/trackers/:ids/events" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-events (merge request {:tracker_ids ids})))
           ))

  ;; TODO missing regexp for id list and order
  (OPTIONS ["/trackers/:ids/events/:order" :order #"latest|latestStored"] [ids latest]
           (-> #'success-handler))
  (GET ["/trackers/:ids/events/:order" :order #"latest|latestStored"] [ids order]
       (-> (fn [request]
             (let [order-by (cond (= order "latest") :latest-event-time
                                  (= order "latestStored") :latest-store-time)]
               (client-api/fetch-events (merge request {:tracker_ids ids :order-by order-by}))))
           ))
  
  (OPTIONS ["/trackers/:ids/sessions" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/trackers/:ids/sessions" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-session (merge request {:tracker_ids ids})))
           ))

  (OPTIONS ["/sessions/:ids/events" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids/events" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-events
                          (merge request {:event_session_ids ids})))
           ))

  (OPTIONS ["/sessions/:ids/events/latest" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids/events/latest" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-events
                          (merge request {:event_session_ids ids})))
           ))
  
  (OPTIONS ["/sessions/:ids" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-session (merge request {:event_session_ids ids})))
           ))

  (OPTIONS ["/sessions/:ids/events" :ids id-list-regex] [ids]
           (-> #'success-handler))
  (GET ["/sessions/:ids/events" :ids id-list-regex] [ids]
       (-> (fn [request] (client-api/fetch-tracker request ids))
           ))
  
  (OPTIONS "/trackers" []
           (-> #'success-handler))
  (GET "/trackers" []
       (-> #'client-api/fetch-trackers ))
  
  (OPTIONS ["/events/:ids" :ids #"([0-9+],?)+"] [ids]
           (-> #'success-handler))
  (GET ["/events/:ids" :ids #"([0-9+],?)+"] [ids]
       (-> (fn [request] (client-api/fetch-event request ids))
           ))

  (OPTIONS "/events" []
           (-> #'success-handler))
  (GET "/events" []
       (-> #'client-api/fetch-events))
  ;; Tracker-API
  (POST "/events" []
        (-> #'tracker-api/handle-create-event)))

(defroutes api-routes
  (context url-prefix []
           (-> api-routes-internal
               (wrap-keyword-params)
               (wrap-params)
               (wrap-json-params)
               (util/wrap-cors-headers)
               (util/wrap-exception-logging)
               (wrap-error-handling)
               (wrap-request-logger)
               (util/wrap-x-forwarded-for)))
  (fn [request] (util/json-error-response request "Resource not found" 404)))
