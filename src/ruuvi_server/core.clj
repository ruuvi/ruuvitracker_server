(ns ruuvi-server.core
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.api :as api]
            [ruuvi-server.util :as util]
            [ring.middleware.gzip :as gzip]
            )
  (:use [compojure.core :only (defroutes GET context)]
        [ring.middleware.reload :only (wrap-reload)]
        [ring.middleware.stacktrace :only (wrap-stacktrace)]
        [clojure.tools.logging :only (debug info warn error)]
        ))

(defroutes main-routes
  (context "/api" [] api/api-routes)
  (-> (route/resources "/")
      util/wrap-add-html-suffix
      util/wrap-dir-index)
  (route/not-found "<h1>Page not found</h1>") )

;; TODO create-prod-application and create-dev-application should be callable in a same way
(def application-prod
  (let [gzip-wrapper (if (get-in (conf/get-config) [:server :enable-gzip])
        gzip/wrap-gzip
        util/wrap-identity) ]
    (-> main-routes
        handler/api
        gzip-wrapper
        util/wrap-x-forwarded-for
        )))

(def application-dev
  (-> application-prod
      (wrap-reload '(ruuvi-server.core))
      (wrap-stacktrace)))

