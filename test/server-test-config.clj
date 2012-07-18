{
 ;; Configuration for unit tests
 :environment :test
 :database {:classname "org.h2.Driver"
            :subprotocol "h2"
            :user "sa"
            :password "sa"
            :subname "mem:ruuvi_server"}
 :server {
          :type :standalone
          :port 8080
          :max-threads 10
          }
 :tracker-api {
               :require-authentication true
               }
 :client-api {
              :max-search-results 50
              }
}