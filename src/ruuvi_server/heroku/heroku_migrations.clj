(ns ruuvi-server.heroku.heroku-migrations
  (:use ruuvi-server.heroku.heroku-config)
  (:use ruuvi-server.models.migration)
  )

(defn -main [direction]
  (migrate direction)
)

