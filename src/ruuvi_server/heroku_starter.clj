(ns ruuvi-server.heroku-starter
  (:use ruuvi-server.core)
  (:import java.net.URI)
  )

;; heroku starter
(defn -main []
  ;; Heroku DATABASE_URL looks like this:
  ;; postgres://username:password@some.host.at.amazonaws.com/databasename
  (let [port (Integer/parseInt (System/getenv "PORT"))
        uri (URI. (System/getenv "DATABASE_URL"))
        splitted-userinfo (.split (.getUserInfo uri) ":")
        database-config {:classname "org.postgresql.Driver"
                         :subprotocol "postgresql"
                         :user (nth (splitted-userinfo) 0)
                         :password (nth (splitted-userinfo) 1)
                         :subname (str "//" (.getHost uri) (.getPath uri))}]
    (start {:database-config database-config
            :server-port port} )))