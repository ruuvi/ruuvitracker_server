(ns ruuvi-server.util
    (:import [org.joda.time.format DateTimeFormat DateTimeFormatter]
           [org.joda.time DateTime])
)

(def date-time-formatter (DateTimeFormat/forPattern "YYYY-MM-dd'T'HH:mm:ss.SSSZ"))
(defn timestamp [] (.print date-time-formatter (new org.joda.time.DateTime)))
