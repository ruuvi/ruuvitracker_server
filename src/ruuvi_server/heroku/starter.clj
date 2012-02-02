(ns ruuvi-server.heroku.starter
  (:use ruuvi-server.core)
  (:use ruuvi-server.heroku.config)
  )

;; heroku webapp starter
(defn -main []
  (start {:server-port *server-port*
          :database-config *database-config*})
  )
