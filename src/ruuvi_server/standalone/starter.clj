(ns ruuvi-server.standalone.starter
  (:use ruuvi-server.core)
  (:use ruuvi-server.standalone.config)
  )
;; USAGE:
;;   lein run -m ruuvi-server.standalone.starter prod
;;   lein run -m ruuvi-server.standalone.starter dev
;; standalone webapp starter
(defn -main [arg]
  ;; TODO this executes start-dev
  ;; should only execute start-dev if some environment variable is set
  (init-config)
  (if (= arg "prod")
    (start-prod {:server-port *server-port*
                :database-config *database-config*})
    (start-dev {:server-port *server-port*
                :database-config *database-config*})
    )
  )