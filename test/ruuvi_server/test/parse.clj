(ns ruuvi-server.test.parse
  (:use [midje.sweet :only (fact throws)]
        [clj-time.core :only (date-time time-zone-for-id to-time-zone)]
        ruuvi-server.parse)
  (:import [java.lang IllegalArgumentException])
  )

(def params {:a "value 1" :b "value 2" :c nil :d nil})

(defn- to-int[val]
  (try
    (Integer/valueOf val)
    (catch NumberFormatException e
      (throw (IllegalArgumentException. (str val " is not an integer"))))
      ))

(fact (parse-parameters {} {}) => {})

(fact (parse-parameters {} {:a {:parser identity}}) => {})

(fact (parse-parameters {:a 1} {:a {:parser identity}}) => {:a {:parsed-value 1}})

(fact (parse-parameters {:a 1 :b 2} {:a {:parser identity}}) => {:a {:parsed-value 1}})

(fact (parse-parameters {:a 1 :b "2"} {:a {:parser identity} :b {:parser to-int}})
      => {:a {:parsed-value 1} :b {:parsed-value 2}})

(fact (parse-parameters {:a "foobar"} {:a {:parser to-int}})
      => {:a {:error "foobar is not an integer"}})

(fact (parse-parameters {:a "foobar" :b "42" :c "bad" :d nil :f "foo"}
                        {:a {:parser to-int} :b {:parser to-int}
                         :c {:parser to-int} :d {:parser identity} :e {:parser to-int}})
      => {:a {:error "foobar is not an integer"}
          :b {:parsed-value 42}
          :c {:error "bad is not an integer"}
          :d {:parsed-value nil}})

(fact (parse-parameters {} {:a {:parser to-int :required true}}) => {:a {:error "Field :a is required."}})

(fact (parse-parameters {:a "2"} {:a {:parser to-int :required true}}) => {:a {:parsed-value 2}})

(fact (parse-parameters {:a "2"} {:a {:parser to-int :required false}}) => {:a {:parsed-value 2}})

(def non-valid-values1 (parse-parameters {:a "foobar"} {:a {:parser to-int}}))
(def non-valid-values2 (parse-parameters {:a "foobar" :b "a" :c "1"}
                                         {:a {:parser to-int} :b {:parser identity} :c {:parser identity}}))
(def valid-values1 (parse-parameters {:a "32"} {:a {:parser to-int}}))
(def valid-values2 (parse-parameters {:a "32" :b "x"} {:a to-int :b {:parser identity}}))

;; is-valid?
(fact (is-valid? non-valid-values1) => false)

(fact (is-valid? non-valid-values2) => false)

(fact (is-valid? valid-values1) => true)

(fact (is-valid? valid-values2) => true)

;; get-value/get-error
(fact (get-value non-valid-values1 :a) => nil)

(fact (get-error valid-values1 :a) => nil)

(fact (get-value valid-values1 :a) => 32)

(fact (get-error non-valid-values1 :a) => "foobar is not an integer")

;; parse functions
(fact (parse-integer "42") => 42)
(fact (parse-integer "x") => (throws IllegalArgumentException) )


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
      (parse-coordinate nil)
      => (throws IllegalArgumentException "Expected coordinate in NMEA or degrees decimal format."))

(fact "parse-coordinate parses empty string to nil"
      (parse-coordinate "")
      => (throws IllegalArgumentException "Expected coordinate in NMEA or degrees decimal format."))

(fact "parse-coordinate parses decimal string to BigDecimal"
      (parse-coordinate "3.14") => 3.14M)

(fact "parse-coordinate parses NMEA string to BigDecimal"
      (parse-coordinate "839.225,W") => -8.653750M)

(fact "parse-coordinate parses BigDecimal value as degrees decimal"
      (parse-coordinate 3.14M) => 3.14M)

(fact "parse-coordinate parses float value as degrees decimal"
      (parse-coordinate 3.14) => 3.14M)

(fact "parse-coordinate parses integer value as degrees decimal"
      (parse-coordinate 3) => 3M)

;; decimal parsing
(fact (parse-decimal "0.123") => 0.123M)
(fact (parse-decimal 0.5) => 0.5M)
(fact (parse-decimal 1) => 1M)
(fact (parse-decimal 1.5M) => 1.5M)

(fact (parse-decimal nil) => (throws IllegalArgumentException "Expected decimal number, got "))

(fact (parse-decimal "foobar") => (throws IllegalArgumentException "Expected decimal number, got foobar") )


;; time and date parsing

(def local-timezone (time-zone-for-id "Europe/Helsinki"))
(def utc-timezone (time-zone-for-id "UTC"))
(def local-date-time (to-time-zone (date-time 2012, 3, 18, 23, 25, 9, 213) local-timezone))
(def parsed-date-time (to-time-zone (date-time 2012, 3, 18, 21, 25, 9, 213) utc-timezone))
(def parsed-date-time-no-millis (to-time-zone (date-time 2012, 3, 18, 21, 25, 9, 0) utc-timezone))
(def date-time-text "2012-03-18T21:25:09.213+0000")

(fact (parse-date-time date-time-text) => parsed-date-time)

(fact (parse-date-time "foob") => (throws IllegalArgumentException "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) format."))

(fact (parse-date-time nil) => (throws IllegalArgumentException "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) format."))

(fact (parse-timestamp "non-timestamp-value") => (throws IllegalArgumentException "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) or Unix timestamp format."))

(fact (parse-timestamp "1332105909") => parsed-date-time-no-millis)
(fact (parse-timestamp "1332105909.000") => parsed-date-time-no-millis)
(fact (parse-timestamp "1332105909.213") => parsed-date-time)

(fact (parse-timestamp date-time-text) => parsed-date-time) 

(fact (parse-unix-timestamp "1332105909") => parsed-date-time-no-millis)
(fact (parse-unix-timestamp "1332105909.000") => parsed-date-time-no-millis)
(fact (parse-unix-timestamp "1332105909.213") => parsed-date-time)


(fact (parse-unix-timestamp "foo") => (throws IllegalArgumentException "Expected Unix timestamp format."))
