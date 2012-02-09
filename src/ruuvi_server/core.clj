(ns ruuvi-server.core
  (:require [ruuvi-server.api :as api])
  (:use ruuvi-server.common)
  (:use ruuvi-server.database)
  (:require [ruuvi-server.models.entities :as db])
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use ring.middleware.json-params)
  (:use ring.middleware.params)
  (:use ring.middleware.session)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(def api-doc-response
  (str "<h1>RuuviTracker API</h1>"
       "<p>API messages</p>"
       "<ul>"
       " <li><a href='api/ping'>ping</a></li>"
       "<ul>"
       ))

(defroutes main-routes
  (GET "/" [] "<h1>Hello World</h1><p>API messages ")
  (GET "/api" [] api-doc-response)
  (context "/api" [] api/api-routes)
  (route/resources "/")
  (route/not-found "<h1>not found</h1>")
  )

(def application
  (-> (handler/site main-routes)
      ))

(def dev-application
  (-> #'application
      (wrap-reload '(ruuvi-server.core))
      (wrap-stacktrace)))

(defn- init-db [config]
  (info "Creating a connection pool to database.")
  (db/map-entities (create-connection-pool (config :database-config))))

(defn start [config]
  (init-db config)
  (let [port (config :server-port)]
    (info "Server (production) on port" port "starting")  
    (run-jetty application {:port port :join? false}))
  )

(defn start-dev [config]
  (init-db config)
  (let [port (config :server-port)]
    (info "Server (development) on port" port "starting")
    (run-jetty dev-application {:port port :join? false}))
  )
