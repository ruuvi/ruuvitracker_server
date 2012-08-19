(ns ruuvi-server.standalone.starter
  (:use ruuvi-server.core)
  (:require [ruuvi-server.configuration :as conf]
            [ruuvi-server.database.entities :as entities])
  (:gen-class))
;; USAGE:
;;   lein run -m ruuvi-server.standalone.starter prod
;;   lein run -m ruuvi-server.standalone.starter dev
;; standalone webapp starter
(defn -main [arg]
  (prn "RuuviServer starting")
  ;; TODO this executes start-dev
  ;; should only execute start-dev if some environment variable is set
  (conf/init-config)
  (entities/init)
  (if (= arg "prod")
    (start-prod)
    (start-dev)
    )
  )