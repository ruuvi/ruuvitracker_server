(ns ruuvi-server.test.parse
  (:use ruuvi-server.parse)
  (:use midje.sweet)
  )

(def params {:a "value 1" :b "value 2" :c nil :d nil})

(defn- to-int[val]
  (try
    (Integer/valueOf val)
    (catch NumberFormatException e
      (throw (IllegalArgumentException. (str val " is not an integer"))))
      ))

(fact (parse-parameters {} {}) => {})

(fact (parse-parameters {} {:a {:parser identity}}) => {})

(fact (parse-parameters {:a 1} {:a {:parser identity}}) => {:a {:parsed-value 1}})

(fact (parse-parameters {:a 1 :b 2} {:a {:parser identity}}) => {:a {:parsed-value 1}})

(fact (parse-parameters {:a 1 :b "2"} {:a {:parser identity} :b {:parser to-int}})
      => {:a {:parsed-value 1} :b {:parsed-value 2}})

(fact (parse-parameters {:a "foobar"} {:a {:parser to-int}})
      => {:a {:error "foobar is not an integer"}})

(fact (parse-parameters {:a "foobar" :b "42" :c "bad" :d nil :f "foo"}
                        {:a {:parser to-int} :b {:parser to-int}
                         :c {:parser to-int} :d {:parser identity} :e {:parser to-int}})
      => {:a {:error "foobar is not an integer"}
          :b {:parsed-value 42}
          :c {:error "bad is not an integer"}
          :d {:parsed-value nil}})

(fact (parse-parameters {} {:a {:parser to-int :required true}}) => {:a {:error "Field :a is required."}})

(fact (parse-parameters {:a "2"} {:a {:parser to-int :required true}}) => {:a {:parsed-value 2}})

(fact (parse-parameters {:a "2"} {:a {:parser to-int :required false}}) => {:a {:parsed-value 2}})

(def non-valid-values1 (parse-parameters {:a "foobar"} {:a {:parser to-int}}))
(def non-valid-values2 (parse-parameters {:a "foobar" :b "a" :c "1"}
                                         {:a {:parser to-int} :b {:parser identity} :c {:parser identity}}))
(def valid-values1 (parse-parameters {:a "32"} {:a {:parser to-int}}))
(def valid-values2 (parse-parameters {:a "32" :b "x"} {:a to-int :b {:parser identity}}))

;; is-valid?
(fact (is-valid? non-valid-values1) => false)

(fact (is-valid? non-valid-values2) => false)

(fact (is-valid? valid-values1) => true)

(fact (is-valid? valid-values2) => true)

;; get-value/get-error
(fact (get-value non-valid-values1 :a) => nil)

(fact (get-error valid-values1 :a) => nil)

(fact (get-value valid-values1 :a) => 32)

(fact (get-error non-valid-values1 :a) => "foobar is not an integer")