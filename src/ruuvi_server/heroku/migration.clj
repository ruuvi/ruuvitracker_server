(ns ruuvi-server.heroku.migration
  (:use ruuvi-server.heroku.config)
  (:use ruuvi-server.database)
  (:use lobos.migrations)
  (:require (lobos [core :as core]))
  )

(defn -main [direction]
  (do-migration (keyword direction) *database-config*)
)

