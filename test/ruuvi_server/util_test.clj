(ns ruuvi-server.util-test
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

(fact ((wrap-strip-trailing-slash identity) {:path-info "/foo"} ) => {:path-info "/foo" }
      ((wrap-strip-trailing-slash identity) {:path-info "/foo/"} ) => {:path-info "/foo" }
      ((wrap-strip-trailing-slash identity) {:path-info "/foo//"} ) => {:path-info "/foo" }
      ((wrap-strip-trailing-slash identity) {:path-info "/path/foo"} ) => {:path-info "/path/foo" }
      )



(fact ":remote-addr value is replaced from x-forwarded-for value"
      ((wrap-x-forwarded-for identity) {:headers {"x-forwarded-for" "example.com"}})
      => {:remote-addr "example.com" :headers {"x-forwarded-for" "example.com"}}
      ((wrap-x-forwarded-for identity) {:headers {"x-forwarded-for" "example.com"}
                                        :remote-addr "127.0.0.1"})
      => {:remote-addr "example.com" :headers {"x-forwarded-for" "example.com"}})
(fact ":remote-addr is not changed if x-forwarded-for header is not present"
      ((wrap-x-forwarded-for identity) {:remote-addr "123.123.123.123"})
      => {:remote-addr "123.123.123.123"})

(fact "wrap-identity does not change request"
      ((wrap-identity identity) {:a 1}) => {:a 1})

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


(fact (json-error-response {:params {}} "error-msg1" 404) =>
      {:status 404, :headers {"Content-Type" "application/json;charset=UTF-8"},
       :body "{\"error\":{\"message\":\"error-msg1\"}}"} )
