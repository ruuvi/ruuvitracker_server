{
 ;; Configuration for standalone server
 :environment :dev
 :database {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :user "ruuvi"
            :password "ruuvi"
            :subname "//localhost/ruuvi_server"}
 :server {
          :port 8080
          :max-threads 80
          }
 :tracker-api {
               :require-authentication true
               }
 :client-api {
              :max-search-results 50
              }
}