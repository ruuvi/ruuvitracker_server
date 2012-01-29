(ns ruuvi-server.core
  (:require [ruuvi-server.api :as api])
  (:use ruuvi-server.common)
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use ring.middleware.json-params)
  (:use ring.middleware.params)
  (:use ring.middleware.session)
  )

(def logger (org.slf4j.LoggerFactory/getLogger "ruuvi-server.core"))

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

(defn start [port]
  (run-jetty application {:port (or port 8080) :join? false}))

(defn start-dev [port]
  (run-jetty dev-application {:port (or port 8080) :join? false}))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (start port)))