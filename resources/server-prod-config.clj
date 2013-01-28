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
          :engine :aleph
          :port 8080
          :max-threads 80
          :enable-gzip true
          :websocket true
          }
 :tracker-api {
               :require-authentication true
               :allow-tracker-creation false
               }
 :client-api {
              :default-max-search-results 100
              :allowed-max-search-results 1000
              }
}