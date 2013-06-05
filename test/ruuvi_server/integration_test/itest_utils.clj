(ns ruuvi-server.integration-test.itest-utils
  (:require [clj-http.client :as http]
                [cheshire.core :as json]
                ) )

(defn http-get [port path]
  (let [no-retry (fn [ex try-count http-context] false)
        opts {:retry-handler no-retry}]
    (:body (http/get (str "http://localhost:" port path) opts))))

(defn http-post [port path body]
  (let [no-retry (fn [ex try-count http-context] false)
        opts {:retry-handler no-retry
              :body (json/generate-string body)
              :content-type :json
              :throw-exceptions false}]
    (:body (http/post (str "http://localhost:" port path) opts))))

(defn http-delete [port path]
  (let [no-retry (fn [ex try-count http-context] false)
        opts {:retry-handler no-retry}]
    (:body (http/delete (str "http://localhost:" port path) opts))))

  
(defn json-get [port path]
  (let [body (http-get port path)]
    (json/parse-string body true) ))

(defn json-post [port path body]
  (let [result (http-post port path body)]
    (json/parse-string result true) ))

(defn json-delete [port path]
  (let [result (http-delete port path)]
    (json/parse-string result true) ))

