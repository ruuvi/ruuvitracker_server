(ns ruuvi-server.test.util
  (:import [org.joda.time DateTime DateTimeUtils])
  (:use ruuvi-server.util)
  (:use midje.sweet)
  )

(def date-time (DateTime. 2012, 3, 18, 23, 25, 9,213))
(def date-time-text "2012-03-18T23:25:09.213+0200")
(fact "date-time-formatter formats a DateTime object as a string"
      (.print date-time-formatter date-time) => date-time-text)

(fact "date-time-formatter parses a string to DateTime object"
      (.parseDateTime date-time-formatter date-time-text) => date-time)

(try
  (DateTimeUtils/setCurrentMillisFixed  (.getMillis date-time))
  (fact "timestamp returns current time formatted as a string"
        (timestamp) => date-time-text)  
  (finally (DateTimeUtils/setCurrentMillisSystem )))

