(ns ruuvi-server.heroku.config
  (:import java.net.URI)
  (:use korma.db)
  (:use ruuvi-server.database)
  (:use ruuvi-server.models.entities)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn init-config []
  ;; TODO fail gracefully when not executing in Heroku environment
  (info "Environment DATABASE_URL =" (System/getenv "DATABASE_URL"))
  (info "Environment PORT =" (System/getenv "PORT"))

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
  (defdb db (postgres *database-config*))
  (init-entities)
)
