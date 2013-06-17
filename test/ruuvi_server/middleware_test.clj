(ns ruuvi-server.middleware-test
  (:require [clojure.java.io :as io]
            [ruuvi-server.user-api :as user-api])
  (:use [ruuvi-server.middleware]
        [midje.sweet]) )

(defn- test-handler [req] nil)

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

(fact "When user is not authenticated, auth-data is not added to request."
 ((wrap-authentication (fn [req] {:body {:some :data}})) {:some :data}) 
 => {:body {:some :data}})

(fact "When user-api doesn't provide auth-data it is not added to request."
 ((wrap-authentication test-handler) {:session {:user-id 42}})
 => {:body {:some :data}}
 (provided (#'ruuvi-server.user-api/auth-data 42) => nil
           (test-handler {:session {:user-id 42}}) => {:body {:some :data}}))

(fact "When handler sets authenticated value, wrap-authentication doesn't change it."
 ((wrap-authentication test-handler) {:session {:user-id 42}})
 => {:body {:authenticated false :some :data}}
 (provided (#'ruuvi-server.user-api/auth-data 42) => {:user-id 42 :group-ids [1 2]}
           (test-handler {:auth-data {:user-id 42 :group-ids [1 2]}
                          :session {:user-id 42}}) => {:body {:authenticated false 
                                                              :some :data}}))

(fact "When user is authenticated, wrap-authentication adds :auth-data to request and  :authenicated true to response body."
 ((wrap-authentication test-handler) {:session {:user-id 42}})
 => {:body {:authenticated true :some :data}}
 (provided (#'ruuvi-server.user-api/auth-data 42) => {:user-id 42 :group-ids [1 2]}
           (test-handler {:auth-data {:user-id 42 :group-ids [1 2]}
                          :session {:user-id 42}}) => {:body {:some :data}}))

