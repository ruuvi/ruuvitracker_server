(ns ruuvi-server.services
  "Functions to interact with external services."
  (:require [clj-http.client :as client])
  )

(defn reverse-geocode
  "Reverse geo-coding (location -> address) using OpenStreetmap Nominatim service"
  [{:keys [lat lon]}]
  (let [nominatim-url "http://nominatim.openstreetmap.org/reverse"
        query-params {:lat lat :lon lon :format "json"}
        response (client/get nominatim-url {:query-params query-params 
                                            :as :json})
        address (get-in response [:body :address])]
    address ))
