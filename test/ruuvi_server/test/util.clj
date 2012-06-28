(ns ruuvi-server.test.util
  (:import [org.joda.time DateTime DateTimeUtils])
  (:use ruuvi-server.util)
  (:use midje.sweet)
  (:import org.joda.time.DateTimeZone)
  )

(def local-timezone (DateTimeZone/forID "Europe/Helsinki"))
(def utc-timezone (DateTimeZone/forID "UTC"))
(def local-date-time (DateTime. 2012, 3, 18, 23, 25, 9, 213, local-timezone))
(def parsed-date-time (DateTime. 2012, 3, 18, 21, 25, 9, 213, utc-timezone))
(def parsed-date-time-no-millis (DateTime. 2012, 3, 18, 21, 25, 9, 0, utc-timezone))
(def date-time-text "2012-03-18T21:25:09.213+0000")

;; decimal parsing
(fact "parse-decimal parses a valid string to BigDecimal object"
      (parse-decimal "0.123") => (BigDecimal. "0.123"))

(fact "parse-decimal parses a nil to nil"
      (parse-decimal nil) => nil)

(fact "parse-decimal parses a non valid string to nil"
      (parse-decimal "foobar") => nil)

;; date and time parsing
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

(fact (parse-timestamp nil) => nil)

(fact (parse-timestamp "non timestamp value") => nil)

(fact (parse-timestamp "1332105909") => parsed-date-time-no-millis)

(fact (parse-timestamp date-time-text) => parsed-date-time) 
                                                                                
(try
  (DateTimeUtils/setCurrentMillisFixed  (.getMillis local-date-time))
  (fact "timestamp returns current time formatted as a string"
        (timestamp) => date-time-text)  
  (finally (DateTimeUtils/setCurrentMillisSystem )))

(fact "timestamp? returns true for valid timestamp string"
      (timestamp? date-time-text) => true)

(fact "timestamp? returns false for invalid timestamp string"
      (timestamp? "xx2011-12-00") => false)

(fact "timestamp? returns true for nil"
      (timestamp? nil) => false)

;; map handling
(fact "remove-nil-values returns nil as nil"
      (remove-nil-values nil) => nil)

(fact "remove-nil-values removes nils and empty values from top level"
      (remove-nil-values
       {nil :x :a false :b nil :c 2 :d {} :e [] :f [1 2] :g {:a1 1 :b2 {} }})
      => {nil :x :a false :c 2 :f [1 2] :g {:a1 1 :b2 {} }})

;; nmea-latitude
(fact "nmea-latitude? returns true for valid latitude string 5839.225,N"
      (nmea-latitude? "5839.225,N") => true)

(fact "nmea-latitude? returns true for valid latitude string 5839.225,s"
      (nmea-latitude? "5839.225,s") => true)

(fact "nmea-latitude? returns false for nil"
      (nmea-latitude? nil) => false)

(fact "nmea-latitude? returns false for 3.14"
      (nmea-latitude? "3.14") => false)

(fact "nmea-latitude? returns false for longitude string 5839.225,E"
      (nmea-latitude? "5839.225,E") => false)

(fact "nmea-latitude? returns false for longitude string 5839.225,W"
      (nmea-latitude? "5839.225,W") => false)

;; nmea-longitude?
(fact "nmea-longitude? returns true for valid longitude string 5839.225,W"
      (nmea-longitude? "5839.225,W") => true)

(fact "nmea-longitude? returns true for valid longitude string 5839.225,e"
      (nmea-longitude? "5839.225,e") => true)

(fact "nmea-longitude? returns false for nil"
      (nmea-longitude? nil) => false)

(fact "nmea-longitude? returns false for 3.14"
      (nmea-longitude? "3.14") => false)

(fact "nmea-longitude? returns false for latitude string 5839.225,S"
      (nmea-longitude? "5839.225,S") => false)

(fact "nmea-longitude? returns false for latitude string 5839.225,N"
      (nmea-longitude? "5839.225,N") => false)

;; parse-nmea-coordinate
(fact "parse-nmea-coordinate converts 5839.225,N to +58.653750"
      (parse-nmea-coordinate "5839.225,N") => 58.653750M)

(fact "parse-nmea-coordinate converts 5839.225,s to -58.653750"
      (parse-nmea-coordinate "5839.225,s") => -58.653750M)

(fact "parse-nmea-coordinate converts 839.225,E to +8.653750"
      (parse-nmea-coordinate "839.225,E") => +8.653750M)

(fact "parse-nmea-coordinate converts 839.225,W to -8.653750"
      (parse-nmea-coordinate "839.225,W") => -8.653750M)

(fact "parse-coordinate parses nil to nil"
      (parse-coordinate nil) => nil)

(fact "parse-coordinate parses empty string to nil"
      (parse-coordinate "") => nil)

(fact "parse-coordinate parses decimal string to BigDecimal"
      (parse-coordinate "3.14") => 3.14M)

(fact "parse-coordinate parses NMEA string to BigDecimal"
      (parse-coordinate "839.225,W") => -8.653750M)

