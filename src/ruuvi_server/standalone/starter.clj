(ns ruuvi-server.standalone.starter
  (:use ruuvi-server.core)
  (:use ruuvi-server.standalone.config)
  )

;; standalone webapp starter
(defn -main []
  (start-dev {:server-port *server-port*
              :database-config *database-config*})
  )