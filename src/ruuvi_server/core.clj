(ns ruuvi-server.core
  (:require [ruuvi-server.configuration :as conf])
  (:require [ruuvi-server.api :as api])
  (:use ruuvi-server.common)
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(def api-doc-response
  (str "<h1>RuuviTracker API</h1>"
       "<p>API messages</p>"
       "<ul>"
       " <li><a href='api/v1-dev/ping'>ping</a></li>"
       " <li><a href='api/v1-dev/trackers'>trackers</a></li>"
       " <li><a href='api/v1-dev/events'>events</a></li>"
       "</ul>"
       "<ul>"
       "  <li><a href='api/v1-dev/trackers/4'>Tracker 4</a></li>"
       "  <li><a href='api/v1-dev/trackers/4/events'>Tracker 4 events</a></li>"
       "  <li><a href='api/v1-dev/trackers/4/events?maxResults=25&eventTimeStart=2012-03-31T10:37:07.000+0000&eventTimeEnd=2012-03-31T12:37:07.000+0000&storeTimeEnd=1342212213&storeTimeStart=10&jsonp=func'>Tracker 4 events with all options</a></li>"
       "</ul>"
       ))

(defn wrap-add-html-suffix
  "Adds .html URI:s without dots and without / ending"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (and (not (.endsWith % "/")) (< (.indexOf % ".") 0))
                   (str % ".html")
                   %)))))

(defn wrap-dir-index
  "Convert paths ending in / to /index.html"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (.endsWith % "/" )
                   (str % "index.html")
                   %)))))

(defroutes main-routes
  (GET "/api" [] api-doc-response)
  (context "/api" [] api/api-routes)
  (wrap-dir-index (wrap-add-html-suffix (route/resources "/")))
  (route/not-found "<h1>Page not found</h1>")
  )

;; TODO create-prod-application and create-dev-application should be callable in a same way
(def application-prod
  (handler/site main-routes))

(def application-dev
  (-> application-prod
      (wrap-reload '(ruuvi-server.core))
      (wrap-stacktrace)))

(defn start-prod
  "Start server in production mode"
  []
  (let [server (conf/*config* :server)
        max-threads (server :max-threads)
        port (server :port)]  
    (info "Server (production) on port" port "starting")
    (run-jetty application-prod {:port port :join? false :max-threads max-threads}))
  )

(defn start-dev
  "Start server in development mode"
  []
  (let [server (conf/*config* :server)
        max-threads (server :max-threads)
        port (server :port)]
    (info "Server (development) on port" port "starting")
    (run-jetty application-dev {:port port :join? false :max-threads max-threads}))
  )

(defn ring-init []
  (info "Initializing ring"))

(defn ring-destroy []
  (info "Finishing ring"))

(defn ring-handler [req]
  (if (= :prod (:environment conf/*config*))
    (application-prod req)
    (application-dev req)))