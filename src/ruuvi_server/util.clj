(ns ruuvi-server.util
    (:import [java.lang IllegalArgumentException]
             [java.math BigDecimal RoundingMode]
             )
    (:require [clojure.walk :as walk]
              [cheshire.core :as json]
              [ruuvi-server.parse :as parse]
              )
    (:use [clojure.tools.logging :only (debug info warn error)]
          [clj-time.coerce :only (from-date)]
          )
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

(defn- object-to-string
  "Convert objects in map to strings, assumes that map is flat"
  [data-map]
  (walk/prewalk (fn [item]
                  (cond (instance? java.util.Date item) (.print parse/date-time-formatter (from-date item))
                        (instance? java.math.BigDecimal item) (str item)
                        :else item)
                  ) data-map))

(defn string-to-ids [value]
  (when value
    (let [strings (.split value ",")
          ids (map #(Integer/parseInt %) strings)]
      ids
    )))

(defn json-response
  "Formats data map as JSON" 
  [request data & [status]]
  (let [params (:params request)
        jsonp-function (:jsonp params)
        pretty {:pretty (:prettyPrint params)}
        converted-data (object-to-string data)
        body (if jsonp-function
              (str jsonp-function "(" (json/generate-string converted-data pretty) ")")
              (json/generate-string converted-data pretty))]
  {:status (or status 200)
   :headers {"Content-Type" "application/json;charset=UTF-8"}
   :body body}))

(defn json-error-response
  [request message status]
  (let [body {:error {:message message}}]
    (json-response request body status)))


(defn try-times*
  "Executes thunk. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n thunk]
  (loop [n n]
    (if-let [result (try
                      [(thunk)]
                      (catch Exception e
                        (when (zero? n)
                          (throw e))))]
      (result 0)
      (recur (dec n)))))

(defmacro try-times
  "Executes body. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n & body]
  `(try-times* ~n (fn [] ~@body)))
