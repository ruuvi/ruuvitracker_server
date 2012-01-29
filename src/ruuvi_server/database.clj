(ns ruuvi-server.database
  )

(def database-driver-class "org.postgresql.Driver")

(defn init-database
  (.forName Class database-driver-class)
  )

