(ns ruuvi-server.heroku.heroku-starter
  (:use ruuvi-server.core)
  (:use ruuvi-server.heroku.heroku_config)
  (:import java.net.URI)
  )

;; heroku webapp starter
(defn -main []
    (start {:database-config database-config
            :server-port server-port} ))