(defproject ruuvi-server "0.0.1-SNAPSHOT"
  :description "RuuviTracker server"
  :dependencies
  [
   [org.clojure/clojure "1.3.0"]
   [compojure "1.1.0"]
   [ring-json-params "0.1.3"]
   [clj-json "0.5.0"]
   [ring/ring-core "1.1.0"]
   [ring/ring-servlet "1.1.0"]
   [ring/ring-jetty-adapter "1.1.0"]
   ;; [sandbar "0.4.0-SNAPSHOT"]
   ;; [enlive "1.0.0"]
   [org.clojure/tools.logging "0.2.3"
    :exclusions [log4j/log4j
                 commons-logging/commons-logging
                 org.slf4j/slf4j-api
                 org.slf4j/slf4j-log4j12]]
   ;;; java deps
   [joda-time/joda-time "2.1"]
   [ch.qos.logback/logback-classic "1.0.3"]
   [org.slf4j/log4j-over-slf4j "1.6.4"]
   [commons-codec/commons-codec "1.6"]

   ;;; database
   [postgresql/postgresql "8.4-702.jdbc4"]
   [org.apache.tomcat/tomcat-jdbc "7.0.25"]
   [korma "0.3.0-beta10" :exclusions [log4j/log4j] ]
   [lobos "1.0.0-SNAPSHOT"]
   [ring/ring-devel "1.1.0"]
   ]
  :dev-dependencies
  [
   [lein-ring "0.7.1"]
   [midje "1.3.1"]
   [lein-midje "1.0.8"]
   [com.h2database/h2 "1.3.167"]
   [marginalia "0.3.2"]
   ]
  
  :main ruuvi-server.standalone.starter
  ;; ahead of time compile
  :aot [ruuvi-server.standalone.starter]
  ;; exclude digital signature files for uberjars
  ;; http://stackoverflow.com/questions/7892244/leiningen-has-problems-building-a-working-uberjar
  :uberjar-exclusions [#"ECLIPSEF.SF"] 
  :ring {:handler ruuvi-server.core/ring-handler
         :init ruuvi-server.core/ring-init
         :destroy ruuvi-server.core/ring-destroy
         }
  )


