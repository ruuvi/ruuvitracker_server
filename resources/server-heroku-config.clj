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
              :max-search-results 50
              }
}