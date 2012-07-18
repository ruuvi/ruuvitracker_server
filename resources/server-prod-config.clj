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