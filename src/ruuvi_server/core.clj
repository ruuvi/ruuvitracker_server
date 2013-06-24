(ns ruuvi-server.core
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.rest-routes :as rest-routes]
            [ruuvi-server.middleware :as middleware]
            [ring.middleware.gzip :as gzip]
            )
  (:use [compojure.core :only (defroutes GET context)]
        [ring.middleware.reload :only (wrap-reload)]
        [ring.middleware.stacktrace :only (wrap-stacktrace)]
        [clojure.tools.logging :only (debug info warn error)]
        ))

(defroutes main-routes
  (context "/api" [] rest-routes/api-routes)
  (-> (route/resources "/")
      middleware/wrap-add-html-suffix
      middleware/wrap-dir-index)
  (route/not-found "<h1>Page not found</h1>") )

;; TODO create-prod-application and create-dev-application should be callable in a same way
(def application-prod
  (let [gzip-wrapper (if (get-in (conf/get-config) [:server :enable-gzip])
        gzip/wrap-gzip
        middleware/wrap-identity) ]
    (-> main-routes
        handler/api
        gzip-wrapper
        middleware/wrap-x-forwarded-for
        )))

(def application-dev
  (-> application-prod
      (wrap-reload '(ruuvi-server.core))
      (wrap-stacktrace)))

