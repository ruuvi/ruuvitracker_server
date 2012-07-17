(ns ruuvi-server.standalone.migration
  (:use lobos.migrations)
  (:require (lobos [core :as core]))
  )


(defn -main [direction]
  (do-migration (keyword direction))
  )
