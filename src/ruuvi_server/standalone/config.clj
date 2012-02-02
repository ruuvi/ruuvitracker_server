(ns ruuvi-server.standalone.config)
(def *database-config*
     {:classname "org.postgresql.Driver"
      :subprotocol "postgresql"
      :user "ruuvi"
      :password "ruuvi"
      :subname "//localhost/ruuvi_server"})

(def *server-port* 8080)