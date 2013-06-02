(ns ruuvi-server.integration-test.itest-utils
  (:require [clj-http.client :as http]
                [cheshire.core :as json]
                ) )

(defn http-get [port path]
  (let [no-retry (fn [ex try-count http-context] false)
        opts {:retry-handler no-retry}]
    (:body (http/get (str "http://localhost:" port path) opts))
    ))

(defn http-post [port path body]
  (let [no-retry (fn [ex try-count http-context] false)
        opts {:retry-handler no-retry
              :body (json/generate-string body)
              :content-type :json}]
    (:body (http/post (str "http://localhost:" port path) opts))
    ))

(defn json-get [port path]
  (let [body (http-get port path)]
    (json/parse-string body true)
    ))
