(ns ruuvi-server.util
    (:import [org.joda.time.format DateTimeFormat DateTimeFormatter]
             [org.joda.time DateTime DateTimeZone]
             ))


(def date-time-formatter (.withZone
                          (DateTimeFormat/forPattern "YYYY-MM-dd'T'HH:mm:ss.SSSZ")
                          (DateTimeZone/forID "UTC")
  ))

(defn timestamp [] (.print date-time-formatter (new org.joda.time.DateTime)))

(defn parse-date-time
  "Parses string to DateTime instance. In case of errors, returns nil."
  [date]
  (try
    (.parseDateTime date-time-formatter date)
    (catch Exception e nil)
    )
  )