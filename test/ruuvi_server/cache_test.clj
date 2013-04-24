(ns ruuvi-server.cache-test
  (:require [clojure.core.cache :as ccache])
  (:use ruuvi-server.cache
        midje.sweet)
  )

(def region (create-cache-region "dummy" 4 10000000))
(defn- calc [x] (+ x 1))

(fact (ccache/has? @region 1) => false)
(fact (lookup region 1 calc) => 2)
(fact (ccache/has? @region 1) => true)

(fact (lookup region 2 calc) => 3)
(fact (lookup region 1 calc) => 2)
(fact (ccache/has? @region 1) => true)
(fact (ccache/has? @region 2) => true)
(lookup region 3 calc)
(lookup region 4 calc)
(lookup region 5 calc)
(fact (ccache/has? @region 2) => false)
(fact (ccache/has? @region 1) => true)
(fact (ccache/has? @region 3) => true)

  
