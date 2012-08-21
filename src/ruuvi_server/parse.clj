(ns ruuvi-server.parse
  (:require [clojure.string :as string])
  (:import [org.joda.time.format DateTimeFormat DateTimeFormatter]
           [org.joda.time DateTime DateTimeZone]
           [java.lang IllegalArgumentException]
           [java.math BigDecimal RoundingMode]
           )
  )

(defn- upper-matches-regex? [value regex]
  (if value
    (let [uppercase (.toUpperCase value)]
      (if (re-matches regex uppercase)
        true
        false
        ))
    false))

(def date-time-formatter (.withZone
                          (DateTimeFormat/forPattern "YYYY-MM-dd'T'HH:mm:ss.SSSZ")
                          (DateTimeZone/forID "UTC")
  ))



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

(defmacro parse [body error-msg]
  `(try
     ~body
     (catch Exception e#
       (throw (IllegalArgumentException. ~error-msg)))))

;; parse functions
(defn parse-decimal
  "Parses string to BigDecimal instance. In case of errors, returns nil."
  [value]
  (parse (BigDecimal. value) "Expected decimal number."))

(defn parse-unix-timestamp [value]
  (parse (DateTime. (* 1000 (Long/valueOf value))
                    (DateTimeZone/forID "UTC"))
         "Expected Unix timestamp format."))

(defn parse-date-time
  "Parses string to DateTime instance. In case of errors, returns nil."
  [date]
  (parse
   (.parseDateTime date-time-formatter date)
   "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) format."
   ))

(defn parse-timestamp
  "Parses a string to DateTime instance. 
Supports unix timestamp and YYYY-MM-dd'T'HH:mm:ss.SSSZ"
  [value]
  (parse
   (cond (not value) nil
         (re-matches #"\d+" value) (parse-unix-timestamp value)
         :default (parse-date-time value))
   "Expected a timestamp in YYYY-MM-dd'T'HH:mm:ss.SSSZ (timezone required) or Unix timestamp format."
   ))

;; TODO check that seconds, minutes and degrees are in proper range [0,59]
(defn nmea-latitude? [value]
  (let [regex #"(\d*.?\d*),[NS]"]
    (upper-matches-regex? value regex)))

(defn nmea-longitude? [value]
  (let [regex #"(\d*.?\d*),[EW]"]
    (upper-matches-regex? value regex)))

(defn- is-nmea-coordinate? [value]
  (and value
      (or (nmea-latitude? value)
          (nmea-longitude? value))))

(defn parse-nmea-coordinate [value]
  (parse
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
  (parse (Integer/valueOf value) "Expected an integer."))

(defn parse-coordinate
  "Parses string to a coordinate. String can be degrees decimal or NMEA format"
  [value]
  (parse 
   (cond (is-nmea-coordinate? value) (parse-nmea-coordinate value)
         :default (BigDecimal. value))
   "Expected coordinate in NMEA or degrees decimal format."))
  
