(ns ruuvi-server.heroku.starter
  (:use ruuvi-server.core)
  (:require [ruuvi-server.configuration :as conf]
            [ruuvi-server.database.entities :as entities])
  )

;; heroku webapp starter
(defn -main []
  (conf/init-config)
  (entities/init)
  (start-prod)
  )
