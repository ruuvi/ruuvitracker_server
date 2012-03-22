(ns ruuvi-server.test.util
  (:import [org.joda.time DateTime DateTimeUtils])
  (:use ruuvi-server.util)
  (:use midje.sweet)
  (:import org.joda.time.DateTimeZone)
  )

(def local-timezone (DateTimeZone/forID "Europe/Helsinki"))
(def utc-timezone (DateTimeZone/forID "UTC"))
(def local-date-time (DateTime. 2012, 3, 18, 23, 25, 9,213, local-timezone))
(def parsed-date-time (DateTime. 2012, 3, 18, 21, 25, 9,213, utc-timezone))
(def date-time-text "2012-03-18T21:25:09.213+0000")

(fact "date-time-formatter formats a DateTime object as a string"
      (.print date-time-formatter local-date-time) => date-time-text)

(fact "date-time-formatter parses a string to DateTime object"
      (.parseDateTime date-time-formatter date-time-text) => parsed-date-time)

(fact "parse-date-time parses a valid string to DateTime object"
      (parse-date-time date-time-text) => parsed-date-time)

(fact "parse-date-time parses invalid string to nil"
      (parse-date-time "foob") => nil)

(fact "parse-date-time parses nil to nil"
      (parse-date-time nil) => nil)

(try
  (DateTimeUtils/setCurrentMillisFixed  (.getMillis local-date-time))
  (fact "timestamp returns current time formatted as a string"
        (timestamp) => date-time-text)  
  (finally (DateTimeUtils/setCurrentMillisSystem )))
