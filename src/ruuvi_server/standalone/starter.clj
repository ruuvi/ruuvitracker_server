(ns ruuvi-server.standalone.starter
  (:use ruuvi-server.core)
  )
;; USAGE:
;;   lein run -m ruuvi-server.standalone.starter prod
;;   lein run -m ruuvi-server.standalone.starter dev
;; standalone webapp starter
(defn -main [arg]
  ;; TODO this executes start-dev
  ;; should only execute start-dev if some environment variable is set
  (if (= arg "prod")
    (start-prod)
    (start-dev)
    )
  )