(ns ruuvi-server.util
    (:import [java.lang IllegalArgumentException]
             [java.math BigDecimal RoundingMode]
             )
    (:require [clojure.walk :as walk]
              [cheshire.core :as json]
              [ruuvi-server.parse :as parse]
              [clojure.string :as string]
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


(defn wrap-spy [handler spyname]
  (fn [request]
    (let [incoming (str spyname ":\n Incoming Request:"  request)]
      (info "wrap-spy-incoming" incoming)
      (let [response (handler request)]
        (let [outgoing (str spyname ":\n Outgoing Response Map:" response)]
          (info "wrap-spy-outgoing" outgoing)
          response)))))

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
                         {"Access-Control-Allow-Origin" request-origin
                          "Access-Control-Allow-Headers" "X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept"
                          "Access-Control-Allow-Credentials" "true"
                          "Access-Control-Allow-Methods" "OPTIONS, GET, POST, PUT, DELETE"})})]
      cors-response
      )))

(defn wrap-identity
  [app]
  (fn [request]
    (app request)))


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

(defn wrap-json-response [handler]
  (fn [req]
    (let [response (handler req)
          body (:body response)
          status (or (:status response) 200)
          content-type (get-in response [:headers "Content-Type"])]
      (if (and body (not= content-type "text/plain"))
        (let [json-response (merge response (json-response req body status))]
          (merge json-response
                 {:headers   
                  (merge (:headers json-response) 
                         (:headers response))}))
        response
      ))))

(defn wrap-exception-logging [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (error e "Handling request " (str req) " failed")
        (throw e)))))

(defn wrap-add-html-suffix
  "Adds .html URI:s without dots and without / ending"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (and (not (.endsWith % "/")) (< (.indexOf % ".") 0))
                   (str % ".html")
                   %)))))

(defn wrap-dir-index
  "Convert paths ending in / to /index.html"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (.endsWith % "/" )
                   (str % "index.html")
                   %)))))

(defn wrap-strip-trailing-slash
  "Remove trailing / from paths"
  [handler]
  (fn [req]
    (handler 
     (update-in req [:path-info]
                #(if % (string/replace % #"/+$" "") %)))))
                   

(defn wrap-x-forwarded-for
  "Replace remote-addr -header with X-Forwarded-for -header if available."
  [handler]
  (fn [request]
    (if-let [xff (get-in request [:headers "x-forwarded-for"])]
      (handler (assoc request :remote-addr (last (string/split xff #"\s*,\s*"))))
      (handler request))))

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
