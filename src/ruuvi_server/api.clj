(ns ruuvi-server.api
  (:use ruuvi-server.common)
  (:require [clj-json.core :as json])
  )

(def logger (org.slf4j.LoggerFactory/getLogger "ruuvi-server.api"))

(defn timestamp [] 
  (.toString (new org.joda.time.DateTime)))

(defn json-response
  "Formats data map as JSON
TODO JSON-P" 
  [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn ping-response [req]
  (.info logger (str "ping from " (req :remote-addr) ))
  (json-response {"ruuvi-tracker-protocol-version" "1"
                  "server-software" (str server-name "/" server-version)
                  "time" (timestamp)}))

(defn handle-post-event [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "accepted"}
  )
