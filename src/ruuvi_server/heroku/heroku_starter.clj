(ns ruuvi-server.heroku.heroku-starter
  (:use ruuvi-server.core)
  (:use ruuvi-server.heroku.heroku-config)
  )

;; heroku webapp starter
(defn -main []
    (start {:database-config database-config
            :server-port server-port} ))