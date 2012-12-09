(ns ruuvi-server.test.util
  (:use ruuvi-server.util)
  (:use midje.sweet)
  (:require [clojure.java.io :as io])
  )

;; map handling
(fact "remove-nil-values returns nil as nil"
      (remove-nil-values nil) => nil)

(fact "remove-nil-values removes nils and empty values from top level"
      (remove-nil-values
       {nil :x :a false :b nil :c 2 :d {} :e [] :f [1 2] :g {:a1 1 :b2 {} }})
      => {nil :x :a false :c 2 :f [1 2] :g {:a1 1 :b2 {} }})


(fact (modify-map {:a 1 :b 2} nil nil) => {:a 1 :b 2})

(fact (modify-map {:a 1 :b 2} {:x 1 :y 2} {:x 1 :y 1}) => {:a 1 :b 2})

(fact (modify-map {:a 1 :b 2} {:a :X :b :Y} {:a 42 :b 41}) => {:X 42 :Y 41})

(fact (modify-map {:a 1 :b 2}
                  {:a str :b (fn [x] (str x))}
                  {:a (fn [x] (+ x 1)) :b (fn [x] (+ x 1))})
                  => {":a" 2 ":b" 3})


(fact (stringify-id-fields {}) => {})

(fact (stringify-id-fields {:a 1 :b 2}) => {:a 1 :b 2})

(fact (stringify-id-fields {:id 1 :foo_id 2 :bar_id 42 :foo "bar"})
      => {:id "1" :foo_id "2" :bar_id "42" :foo "bar"})

(fact (stringify-id-fields {:id nil :foo_id nil :bar_id false :foo "bar"})
      => {:id nil :foo_id nil :bar_id false :foo "bar"})


;; wrap-dir-index
(fact "wrap-dir-index appends index.html to end of :uri that ends with /"
      ((wrap-dir-index identity) {:uri "/"} ) => {:uri "/index.html"}
      ((wrap-dir-index identity) {:uri "/some/path/"} ) => {:uri "/some/path/index.html"}
      )

(fact "wrap-dir-index leaves untouched :uri's that do not end with /"
      ((wrap-dir-index identity) {:uri "/path."} ) => {:uri "/path."}
      ((wrap-dir-index identity) {:uri "/some/path"} ) => {:uri "/some/path"}
      ((wrap-dir-index identity) {:uri "/some/path/index.html"} ) => {:uri "/some/path/index.html"})

;; wrap-add-html-suffix
(fact "wrap-add-html-suffix leaves untouched :uri's ending with /"
      ((wrap-add-html-suffix identity) {:uri "/"} ) => {:uri "/" }
      ((wrap-add-html-suffix identity) {:uri "/foo/"} ) => {:uri "/foo/" }
      )

(fact "wrap-add-html-suffix leaves untouched :uri's containing a dot"
      ((wrap-add-html-suffix identity) {:uri "/foo.html"} ) => {:uri "/foo.html" }
      ((wrap-add-html-suffix identity) {:uri "/foo/foo.jpg"} ) => {:uri "/foo/foo.jpg" }
      )

(fact "wrap-add-html-suffix adds '.html' suffix to :uri's that do not have a suffix"
      ((wrap-add-html-suffix identity) {:uri "/foo"} ) => {:uri "/foo.html" }
      ((wrap-add-html-suffix identity) {:uri "/path/foo.html"} ) => {:uri "/path/foo.html" }
      )

(fact "wrap-identity does not change request"
      ((wrap-identity identity) {:a 1}) => {:a 1})