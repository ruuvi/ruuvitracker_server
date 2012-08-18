(defproject ruuvi-server/ruuvi-server "0.0.1-SNAPSHOT" 
  :description "RuuviTracker server"
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.7.3"]
            [lein-midje "2.0.0-SNAPSHOT"]
            [lein-marginalia "0.7.1"]
            ]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.0"]
                 [ring-json-params "0.1.3"]
                 [clj-json "0.5.0"]
                 [ring/ring-core "1.1.0"]
                 [ring/ring-servlet "1.1.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [org.clojure/tools.logging
                  "0.2.3"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [joda-time/joda-time "2.1"]
                 [ch.qos.logback/logback-classic "1.0.3"]
                 [org.slf4j/log4j-over-slf4j "1.6.6"]
                 [commons-codec/commons-codec "1.6"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [com.h2database/h2 "1.3.167"]
                 [com.jolbox/bonecp "0.7.1.RELEASE"]
                 [korma "0.3.0-beta10" :exclusions [log4j/log4j]]
                 [lobos "1.0.0-SNAPSHOT"]
                 [ring/ring-devel "1.1.0"]
                 ]
  :ring {:handler ruuvi-server.core/ring-handler,
         :init ruuvi-server.core/ring-init,
         :destroy ruuvi-server.core/ring-destroy}
  :profiles {:dev
             {:dependencies
              [
               [midje "1.4.0" :exclusions [org.clojure/clojure]]
               [marginalia "0.7.1"]
               ]}}
  :aot [ruuvi-server.standalone.starter]
  :main ruuvi-server.standalone.starter
  )
