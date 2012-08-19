(ns ruuvi-server.standalone.migration
  (:use lobos.migrations)
  (:require (lobos [core :as core]))
  (:require [ruuvi-server.configuration :as conf]
            [ruuvi-server.database.entities :as entities])
  )


(defn -main [direction]
  (conf/init-config)
  (entities/init)
  (do-migration (keyword direction))
  )
