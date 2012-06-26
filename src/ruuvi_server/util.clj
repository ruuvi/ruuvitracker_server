(ns ruuvi-server.util
    (:import [org.joda.time.format DateTimeFormat DateTimeFormatter]
             [org.joda.time DateTime DateTimeZone])
    (:import [java.lang IllegalArgumentException])
    (:import [java.math BigDecimal])
    (:import java.math.RoundingMode)
    )

(defn remove-nil-values
  "Removes keys that have nil values"
  [data-map]
  (let [data (into {}
                   (filter
                    (fn [item]
                      (if item
                        (let [value (item 1)]
                          (cond (and (coll? value) (empty? value)) false
                                (= value nil) false
                                :else true))
                        nil)
                      ) data-map))]
    (if (empty? data)
      nil
      data)
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

(defn timestamp? [value]
  (cond (not value) false
        (parse-date-time value) true
        :else false))

(defn- upper-matches-regex? [value regex]
  (if value
    (let [uppercase (.toUpperCase value)]
      (if (re-matches regex uppercase)
        true
        false
        ))
    false))

;; TODO check that seconds, minutes and degrees are in proper range [0,59]
(defn nmea-latitude? [value]
  (let [regex #"(\d*.?\d*),[NS]"]
    (upper-matches-regex? value regex)))

(defn nmea-longitude? [value]
  (let [regex #"(\d*.?\d*),[EW]"]
    (upper-matches-regex? value regex)))

(defn- is-nmea-coordinate? [value]
  (or (not value)
      (not (nmea-latitude? value))
      (not (nmea-longitude? value))))


(defn nmea-to-decimal [value]
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


(defn wrap-cors-headers
  "http://www.w3.org/TR/cors/"
  [app & methods]
  (fn [request]
    (let [response (app request)
          request-origin (when (:headers request)
                           ((:headers request) "origin")) 
          options (apply str (interpose ", " (conj methods "OPTIONS")))
          cors-response
          (merge response
                 {:headers   
                  (merge (:headers response)
                         {"Access-Control-Allow-Origin" (or request-origin "*")
                          "Access-Control-Allow-Headers" "X-Requested-With, Content-Type, Accept, Origin"
                          "Access-Control-Allow-Methods" options})})]
      cors-response
      )))
