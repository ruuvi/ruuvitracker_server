(ns ruuvi-server.heroku-starter
  (:use ruuvi-server.core)
  )

;; heroku starter
(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (start port)))