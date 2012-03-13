(ns ruuvi-server.standalone.migration
  (:use ruuvi-server.standalone.config)
  (:use ruuvi-server.database)
  (:use lobos.migrations)
  (:require (lobos [core :as core]))
  )


(defn -main [direction]
  (init-config)
  (do-migration (keyword direction) *database-config*)
  )
