(ns ruuvi-server.middleware
  "Compojure middlewares"
  (:require [ruuvi-server.util :as util]
            [clojure.string :as string] )
  (:use [clojure.tools.logging :only (debug info warn error)] )
  (:import com.fasterxml.jackson.core.JsonParseException) )

(defn wrap-spy 
  "Logs request and response objects. For debugging only."
  [handler spyname]
  (fn [request]
    (let [incoming (str spyname ":\n Incoming Request:"  request)]
      (info "wrap-spy-incoming" incoming)
      (let [response (handler request)]
        (let [outgoing (str spyname ":\n Outgoing Response Map:" response)]
          (info "wrap-spy-outgoing" outgoing)
          response)))))

(defn wrap-cors-headers
  "Adds Cross Origin Resource Sharing headers to response
http://www.w3.org/TR/cors/"
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
      cors-response )))

(defn wrap-identity
  "wrap-identity does nothing"
  [app]
  (fn [request]
    (app request)))

(defn wrap-json-response 
  "Converts clojure maps to JSON response."
  [handler]
  (fn [req]
    (let [response (handler req)
          body (:body response)
          status (or (:status response) 200)
          content-type (get-in response [:headers "Content-Type"])]
      (if (and body (not= content-type "text/plain"))
        (let [response-in-json (merge response (util/json-response req body status))]
          (merge response-in-json
                 {:headers   
                  (merge (:headers response-in-json) 
                         (:headers response))}))
        response
      ))))

(defn wrap-exception-logging 
  "Logs uncaught exceptions"
  [handler]
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

(defn wrap-request-logger
  "Logs each incoming request"
  [app request-counter]
  (fn [request]
    (let [counter (swap! request-counter inc)
          request-method (:request-method request)
          uri (:uri request)
          query-params (:query-params request)
          start (System/currentTimeMillis)
          remote-addr (:remote-addr request)
          query (if (not (empty? query-params)) 
                  (str ":query-params "  query-params)
                  "") ]
      (info (str "REQUEST:" counter)
            remote-addr request-method uri query)
      (let [response (app request)
            duration (- (System/currentTimeMillis) start)
            status (:status response)]
        (info (str "RESPONSE:" counter)
              remote-addr
              status
              duration "msec")
        response) )))

(defn wrap-error-handling
  "Catches exceptions and shows them as JSON errors"
  [handler]
  (fn [request]
    (try
      (or (handler request)
          (util/json-response request {"error" "resource not found"} 404))
      (catch JsonParseException e
        (util/json-response request {"error" "malformed json"} 400))
      (catch Exception e
        (util/json-response request {"error" "Internal server error"} 500)))))

(defn wrap-strip-trailing-slash
  "Remove trailing / from paths"
  [handler]
  (fn [req]
    (handler 
     (update-in req [:path-info]
                #(if % (string/replace % #"/+$" "") %)))))
                   
(defn wrap-x-forwarded-for
  "Replace value of remote-addr -header with value of X-Forwarded-for -header if available."
  [handler]
  (fn [request]
    (if-let [xff (get-in request [:headers "x-forwarded-for"])]
      (handler (assoc request :remote-addr (last (string/split xff #"\s*,\s*"))))
      (handler request))))
