(ns ruuvi-server.test.core
  (:use midje.sweet
        ruuvi-server.core))

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
