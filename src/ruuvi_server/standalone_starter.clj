(ns ruuvi-server.standalone-starter
  (:use ruuvi-server.core)
  (:require [ruuvi-server.database-config :as dbconf])
  )


(defn -main []
  (start-dev {:server-port 8080
              :database-config dbconf/dbconfig})
  )