(ns ruuvi-server.util-test
  (:use ruuvi-server.util)
  (:use midje.sweet))

;; remove-nil-values
(fact "remove-nil-values returns nil as nil"
      (remove-nil-values nil) => nil)

(fact "remove-nil-values removes nils and empty values from top level"
      (remove-nil-values
       {nil :x :a false :b nil :c 2 :d {} :e [] :f [1 2] :g {:a1 1 :b2 {} }})
      => {nil :x :a false :c 2 :f [1 2] :g {:a1 1 :b2 {} }})

;; modify-map
(fact (modify-map {:a 1 :b 2} nil nil) => {:a 1 :b 2})

(fact (modify-map {:a 1 :b 2} {:x 1 :y 2} {:x 1 :y 1}) => {:a 1 :b 2})

(fact (modify-map {:a 1 :b 2} {:a :X :b :Y} {:a 42 :b 41}) => {:X 42 :Y 41})

(fact (modify-map {:a 1 :b 2}
                  {:a str :b (fn [x] (str x))}
                  {:a (fn [x] (+ x 1)) :b (fn [x] (+ x 1))})
                  => {":a" 2 ":b" 3})

;; stringify-id-fields
(fact (stringify-id-fields {}) => {})

(fact (stringify-id-fields {:a 1 :b 2}) => {:a 1 :b 2})

(fact (stringify-id-fields {:id 1 :foo_id 2 :bar_id 42 :foo "bar"})
      => {:id "1" :foo_id "2" :bar_id "42" :foo "bar"})

(fact (stringify-id-fields {:id nil :foo_id nil :bar_id false :foo "bar"})
      => {:id nil :foo_id nil :bar_id false :foo "bar"})

;; json-response
(fact (json-response {:params {}} {:a 1} ) =>
      {:status 200, :headers {"Content-Type" "application/json;charset=UTF-8"},
       :body "{\"a\":1}"}

      (json-response {:params {}} {:a 1} 404) =>
      {:status 404, :headers {"Content-Type" "application/json;charset=UTF-8"},
       :body "{\"a\":1}"}
      
      (json-response {:params {:jsonp "callb"}} {:a 1} ) =>
      {:status 200, :headers {"Content-Type" "application/json;charset=UTF-8"},
       :body "callb({\"a\":1})"}

      (json-response {:params {:prettyPrint "true"}} {:a 1} ) =>
      {:status 200, :headers {"Content-Type" "application/json;charset=UTF-8"},
       :body "{\n  \"a\" : 1\n}"}
      ) 
;; json-error-respones
(fact (json-error-response {:params {}} "error-msg1" 404) =>
      {:status 404, :headers {"Content-Type" "application/json;charset=UTF-8"},
       :body "{\"error\":{\"message\":\"error-msg1\"}}"} )
