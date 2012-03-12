(ns ruuvi-server.test.core
  (:use [ruuvi-server.core])
  (:use clojure.test))

(deftest wrap-dir-index-test
  (defn f [param] ((wrap-dir-index identity) param))
  (testing "wrap-dir-index"
    
    (testing "converts URI / to /index.html"
      (is (= (f {:uri "/"})
             {:uri "/index.html"})))

    (testing "converts URI /some/path to /some/path"
      (is (= (f {:uri "/some/path"})
             {:uri "/some/path"})))
    
    (testing "converts URI /some/path/ to /some/path/index.html"
      (is (= (f {:uri "/some/path/"})
             {:uri "/some/path/index.html"})))
    ))

(deftest wrap-add-html-suffix-test
  (defn f [param] ((wrap-add-html-suffix identity) param))
  (testing "wrap-add-html-suffix"
    
    (testing "leaves URI / unconverted"
      (is (= (f {:uri "/"})
             {:uri "/"})))
    
    (testing "converts URI /foo to /foo.html"
      (is (= (f {:uri "/foo"})
             {:uri "/foo.html"})))
    
    (testing "leaves URI /foo. unconverted"
      (is (= (f {:uri "/foo."})
             {:uri "/foo."})))
    
    (testing "leaves URI /foo.html unconverted"
      (is (= (f {:uri "/foo.html"})
             {:uri "/foo.html"})))
    ))