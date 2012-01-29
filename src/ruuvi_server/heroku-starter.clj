(ns ruuvi-server.heroku-starter
  (:user ruuvi-server.core)
  )

;; heroku starter
(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (start port)))