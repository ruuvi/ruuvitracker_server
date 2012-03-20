(ns ruuvi-server.heroku.migration
  (:use ruuvi-server.heroku.config)
  (:use lobos.migrations)
  (:require (lobos [core :as core]))
  )

(defn -main [direction]
  (init-config)
  (do-migration (keyword direction) *database-config*)
)

