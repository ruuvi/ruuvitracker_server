{
 ;; Configuration for standalone server
 :environment :dev
 :database {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :user "ruuvi"
            :password "ruuvi"
            :subname "//localhost/ruuvi_server"}
 
 ;; for H2 support uncomment this
 ;;:database {:classname "org.h2.Driver"
 ;;           :subprotocol "h2"
 ;;           ;; put file where db is stored as :subname
 ;;           ;; most likely you want an absolute path
 ;;           :subname "ruuviserverH2" }


 :server {
          :type :standalone
          :engine :aleph
          :port 8080
          :max-threads 80
          :enable-gzip true
          :websocket true
          }
 :tracker-api {
               :require-authentication false
               :allow-tracker-creation true
               }
 :client-api {
              :default-max-search-results 100
              :allowed-max-search-results 1000
              }
 }