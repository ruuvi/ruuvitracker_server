(ns ruuvi-server.parse
  (:require [clojure.string :as string])
  (:import [java.lang IllegalArgumentException]
           [java.math BigDecimal BigInteger RoundingMode]
           )
  (:use [clj-time.core :only (now time-zone-for-id ) ]
        [clj-time.coerce :only (from-long) ]
        [clj-time.format :only (formatter parse unparse) ])
  )

(defn- upper-matches-regex? [^String value regex]
  (if value
    (let [uppercase (.toUpperCase value)]
      (if (re-matches regex uppercase)
        true
        false
        ))
    false))

(def date-time-formatter (formatter "YYYY-MM-dd'T'HH:mm:ss.SSSZ" (time-zone-for-id "UTC")))

(defn timestamp [] (unparse date-time-formatter (now)))

(defn parse-parameters
  "Params is map fieldname-keyword -> string.
Handlers is a map fieldname-keyword -> {:parser function :required boolean}. Parse
function returns parsed object or throws exception
Returns a map {field-name-keyword {:parsed-value <Value as parsed object}}
or {field-name-keyword {:error 'Error message'}
 {:time {:parsed-value <DateTime>} :latitude {:error 'ABC is not valid latitude value'}}
"
  [params parsers]
  (let [parsed-values
        (for [[field {parser :parser required :required}] parsers]
          (cond
           (and required (string/blank? (params field)))
           [field {:error (str "Field " field " is required.")}]
           
           (contains? params field)
           (try 
             [field {:parsed-value (parser (params field))}]
             (catch Exception e
               [field {:error (.getMessage e)}]
               ))
           
           :default nil))
        ]
    (into {} parsed-values) 
    )
)

(defn is-valid? [parsed-values]
  (let [errors (filter
                (fn [[field data]] (data :error))                          
                parsed-values)]
    (empty? errors)))

(defn get-value [parsed-values field]
  ((parsed-values field) :parsed-value))

(defn get-error [parsed-values field]
  ((parsed-values field) :error))

(defmacro parse-value [body error-msg]
  `(try
     ~body
     (catch Exception e#
       (throw (IllegalArgumentException. ~error-msg)))))

;; parse functions
(defn- to-big-decimal [value]
  (cond (instance? BigInteger value) (BigDecimal. value)
        (instance? BigDecimal value) value
        (integer? value) (BigDecimal/valueOf value)
        (float? value) (BigDecimal/valueOf value)
        :else nil))

(defn parse-decimal
  "Parses string to BigDecimal instance. In case of errors, returns nil."
  [value]
  (parse-value (let [decimal (to-big-decimal value)]
                 (if decimal
                   decimal
                   (BigDecimal. value)
                   ))
               (str "Expected decimal number, got " value " <" (type value) ">")))

(defn parse-unix-timestamp
  "Parses also fractional seconds."
  [^String value]
  (parse-value 
   (let [decimal (parse-decimal value)
         millisecs (.longValue (* decimal 1000))]
     (from-long millisecs))
     "Expected Unix timestamp format."))

(defn parse-date-time
  "Parses string to DateTime instance. In case of errors, returns nil."
  [^String date]
  (parse-value
   (parse date-time-formatter date)
   "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) format."
   ))

(defn parse-timestamp
  "Parses a string to DateTime instance. 
Supports unix timestamp and YYYY-MM-dd'T'HH:mm:ss.SSSZ"
  [^String value]
  (parse-value
   (cond (not value) nil
         (re-matches #"\d+\.?\d*" value) (parse-unix-timestamp value)
         :default (parse-date-time value))
   "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) or Unix timestamp format."
   ))

;; TODO check that seconds, minutes and degrees are in proper range [0,59]
(defn nmea-latitude? [^String value]
  (let [regex #"(\d*.?\d*),[NS]"]
    (upper-matches-regex? value regex)))

(defn nmea-longitude? [^String value]
  (let [regex #"(\d*.?\d*),[EW]"]
    (upper-matches-regex? value regex)))

(defn- is-nmea-coordinate? [^String value]
  (and value
      (or (nmea-latitude? value)
          (nmea-longitude? value))))

(defn parse-nmea-coordinate [^String value]
  (parse-value
   (do
     (when (not (is-nmea-coordinate? value))
       (throw (IllegalArgumentException. (str value " is not valid NMEA coordinate"))))    
     (let [upper (.toUpperCase value)
           regex #"(\d*)(\d\d.?\d*),([NSWE])"
           match-groups (re-matches regex upper)
           area (match-groups 3)
           sign (if (or (.contains area "S")
                      (.contains area "W"))
                  -1
                  +1)
           degrees (BigDecimal. (match-groups 1))
           minutes (BigDecimal. (match-groups 2))
           ]
       (* sign (+ degrees (.divide minutes 60.0M 6 RoundingMode/FLOOR)))
       ))
   "Expected coordinate in NMEA format"
   ))
  
(defn parse-integer [value]
  (parse-value (Integer/valueOf value) "Expected an integer."))

(defn parse-coordinate
  "Parses string to a coordinate. String can be degrees decimal or NMEA format"
  [value]
  (let [primitive-value (to-big-decimal value)]
    (parse-value 
     (cond
      primitive-value primitive-value
      (is-nmea-coordinate? value) (parse-nmea-coordinate value)
      :default (BigDecimal. value))
     "Expected coordinate in NMEA or degrees decimal format.")))

