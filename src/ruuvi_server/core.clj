(ns ruuvi-server.core
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

;; TODO Currently this assumes that ruuvi-server.standalone.config/init-config
;; or ruuvi-server.heroku.config/init-config has been called prior to using the start functions

(def api-doc-response
  (str "<h1>RuuviTracker API</h1>"
       "<p>API messages</p>"
       "<ul>"
       " <li><a href='api/ping'>ping</a></li>"
       "<ul>"
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
  (route/not-found "<h1>not found</h1>")
  )

;; TODO create-prod-application and create-dev-application should be callable in a same way
(def create-prod-application
  (handler/site main-routes))

(defn create-dev-application []
  (-> create-prod-application
      (wrap-reload '(ruuvi-server.core))
      (wrap-stacktrace)))


(defn start-prod
  "Start server in production mode"
  [config]
  (let [port (config :server-port)]
    (info "Server (production) on port" port "starting")  
    (run-jetty create-prod-application {:port port :join? false}))
  )

(defn start-dev
  "Start server in development mode"
  [config]
  (let [port (config :server-port)]
    (info "Server (development) on port" port "starting")
    (run-jetty (create-dev-application) {:port port :join? false}))
  )
