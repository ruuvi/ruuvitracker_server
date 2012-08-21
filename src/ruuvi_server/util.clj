(ns ruuvi-server.util
    (:import [org.joda.time.format DateTimeFormat DateTimeFormatter]
             [org.joda.time DateTime DateTimeZone])
    (:import [java.lang IllegalArgumentException])
    (:import [java.math BigDecimal])
    (:import java.math.RoundingMode)
    (:require [clojure.walk :as walk]
              [clj-json.core :as json]
              [clj-stacktrace.repl :as strp])
    (:use [clojure.tools.logging :only (debug info warn error)])
    )

(defn modify-map 
  "Goes through all entries in data map and converts values"
  [data key-modifiers value-modifiers]
  (into {}
        (for [[key value] data]
          (let [new-value
                (if (contains? value-modifiers key)
                  (let [modifier (value-modifiers key)]
                    (if (fn? modifier)
                      (modifier value)
                      modifier))
                  value)
                
                new-key
                (if (contains? key-modifiers key)
                  (let [modifier (key-modifiers key)]
                    (if (fn? modifier)
                      (modifier key)
                      modifier))
                  key)]
            [new-key new-value]
            ))))

(defn stringify-id-fields [data]
  (into {}
        (for [[key value] data]
          (let [new-value
                (if (and value (or (= key :id) (.endsWith (str key) "_id")))
                  (str value)
                  value)]
            [key new-value]))))
  
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

(defn parse-decimal
  "Parses string to BigDecimal instance. In case of errors, returns nil."
  [decimal]
  (try
    (BigDecimal. decimal)
    (catch Exception e nil)
    ))

(defn- parse-unix-timestamp [value]
  (DateTime. (* 1000 (Long/valueOf value)) (DateTimeZone/forID "UTC")))

(defn parse-date-time
  "Parses string to DateTime instance. In case of errors, returns nil."
  [date]
  (try
    (.parseDateTime date-time-formatter date)
    (catch Exception e nil)))

(defn parse-timestamp
  "Parses a string to DateTime instance. In case of errors, returns nil.
Supports unix timestamp and YYYY-MM-dd'T'HH:mm:ss.SSSZ"
  [value]
  (cond (not value) nil
        (re-matches #"\d+" value) (parse-unix-timestamp value)
        :default (parse-date-time value)))

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
  (and value
      (or (nmea-latitude? value)
          (nmea-longitude? value))))

(defn parse-nmea-coordinate [value]
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

(defn parse-coordinate
  "Parses string to a coordinate. String can be decimal or NMEA format"
  [value]
  (cond (not value) nil
        (empty? value) nil
        (is-nmea-coordinate? value) (parse-nmea-coordinate value)
        :default (BigDecimal. value)
        ))

(defn wrap-cors-headers
  "http://www.w3.org/TR/cors/"
  [app]
  (fn [request]
    (let [response (app request)
          request-origin (when (:headers request)
                           ((:headers request) "origin")) 
          cors-response
          (merge response
                 {:headers   
                  (merge (:headers response)
                         {"Access-Control-Allow-Origin" (or request-origin "*")
                          "Access-Control-Allow-Headers" "X-Requested-With, Content-Type, Origin, Referer, User-Agent"
                          "Access-Control-Allow-Methods" "OPTIONS, GET, POST, PUT, DELETE"})})]
      cors-response
      )))


(defn- object-to-string
  "Convert objects in map to strings, assumes that map is flat"
  [data-map]
  (walk/prewalk (fn [item]
                  (cond (instance? java.util.Date item) (.print date-time-formatter (DateTime. item))
                        (instance? java.math.BigDecimal item) (str item)
                        :else item)
                  ) data-map))

(defn json-response
  "Formats data map as JSON" 
  [request data & [status]]
  (let [jsonp-function ((request :params) :jsonp)
        converted-data (object-to-string data)
        body (if jsonp-function
              (str jsonp-function "(" (json/generate-string converted-data) ")")
              (json/generate-string converted-data))]
  {:status (or status 200)
   :headers {"Content-Type" "application/json;charset=UTF-8"}
   :body body}))

(defn json-error-response
  [request message status]
  (let [body {:error {:message message}}]
    (json-response request body status)))

(defn wrap-exception-logging [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (error e "Handling request " (str req) " failed")
        (throw e)))))