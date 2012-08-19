(ns ruuvi-server.standalone.populate-database
  (:require [ruuvi-server.database.load-initial-data :as load-initial-data]
            [ruuvi-server.configuration :as conf]
            [ruuvi-server.database.entities :as entities])
  )

(defn -main []
  (conf/init-config)
  (entities/init)
  (load-initial-data/create-test-trackers)
  )
