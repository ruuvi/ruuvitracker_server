{
 ;; Configuration for standalone server
 :environment :prod
 :database {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :user "ruuvi"
            :password "ruuvi"
            :subname "//localhost/ruuvi_server"}
 :server {
          :type :standalone
          :engine jetty
          :port 8080
          :max-threads 80
          :enable-gzip true
          }
 :tracker-api {
               :require-authentication true
               :allow-tracker-creation false
               }
 :client-api {
              :max-search-results 50
              }
}