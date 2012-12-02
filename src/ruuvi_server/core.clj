(ns ruuvi-server.core
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.api :as api]
            [ruuvi-server.util :as util]
            [ruuvi-server.database.entities :as entities]
            [ring.middleware.gzip :as gzip]
            )
  (:use [compojure.core :only (defroutes GET context)]
        [ring.middleware.reload :only (wrap-reload)]
        [ring.middleware.stacktrace :only (wrap-stacktrace)]
        [clojure.tools.logging :only (debug info warn error)]
        )
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
       "  <li><a href='api/v1-dev/sessions/1,2,3'>Sessions 1,2,3</a></li>"
       "  <li><a href='api/v1-dev/sessions/1,2,3/events'>Events for sessions 1,2,3</a></li>"
       "  <li><a href='api/v1-dev/trackers/1,2,3/sessions'>Sessions for trackers 1,2,3</a></li>"
       "</ul>"
       ))

(defroutes main-routes
  (GET "/api" [] api-doc-response)
  (context "/api" [] api/api-routes)
  (-> (route/resources "/")
      util/wrap-add-html-suffix
      util/wrap-dir-index)
  (route/not-found "<h1>Page not found</h1>")
  )

;; TODO create-prod-application and create-dev-application should be callable in a same way
(def application-prod
  (-> main-routes
      handler/api
      gzip/wrap-gzip))

(def application-dev
  (-> application-prod
      (wrap-reload '(ruuvi-server.core))
      (wrap-stacktrace)))
