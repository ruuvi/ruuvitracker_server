(ns ruuvi-server.heroku.starter
  (:use ruuvi-server.core)
  )

;; heroku webapp starter
(defn -main []
  (start-prod)
  )
