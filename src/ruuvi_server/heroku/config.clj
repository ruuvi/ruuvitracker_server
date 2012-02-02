(ns ruuvi-server.heroku.config
  (:import java.net.URI)
  )

(def *database-config*
  ;; Heroku DATABASE_URL looks like this:
  ;; postgres://username:password@some.host.at.amazonaws.com/databasename
  (let [uri (URI. (System/getenv "DATABASE_URL"))
        splitted-userinfo (.split (.getUserInfo uri) ":")]
        {:classname "org.postgresql.Driver"
         :subprotocol "postgresql"
         :user (nth splitted-userinfo 0)
         :password (nth splitted-userinfo 1)
         :subname (str "//" (.getHost uri) (.getPath uri))
         }))

(def *server-port* (Integer/parseInt (System/getenv "PORT")))
