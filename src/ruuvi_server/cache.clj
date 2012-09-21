(ns ruuvi-server.cache
  "Simple cache implementation. Cache has max size and timeout."
  (:require [clojure.core.cache :as ccache]))

(defn- create-cache [size timeout-msec]
  (-> {}
      (ccache/ttl-cache-factory :ttl timeout-msec)
      (ccache/lru-cache-factory :threshold size)
      ))

(defn create-cache-region [name max-items timeout-msec]
  (atom (create-cache max-items timeout-msec)))

(defn lookup
  "fetch-fn is single argument function that returns new content for the id.
fetch-fn takes id as a parameter."
  [cache-region id fetch-fn]
  (let [new-cache (swap! cache-region
                         (fn [cache-store]
                           (if (ccache/has? cache-store id)
                             (ccache/hit cache-store id)
                             (ccache/miss cache-store id (fetch-fn id)))))]
    (ccache/lookup new-cache id)))