{
 ;; Configuration for standalone server
 :environment :prod
 :server {
          :type :heroku
          :max-threads 80
          :enable-gzip true
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