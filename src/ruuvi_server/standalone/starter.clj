(ns ruuvi-server.standalone.starter
  (:use ruuvi-server.core)
  (:use ruuvi-server.standalone.config)
  )

;; standalone webapp starter
(defn -main []
  ;; TODO this executes start-dev
  ;; should only execute start-dev if some environment variable is set
  (init-config)
  (start-dev {:server-port *server-port*
              :database-config *database-config*})
  )