(defproject ruuvi-server/ruuvi-server "0.0.1-SNAPSHOT" 
  :description "RuuviTracker server"
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.7.3"]
            [lein-midje "2.0.0-SNAPSHOT"]
            [lein-marginalia "0.7.1"]
            [org.clojars.llasram/lein-otf "2.0.0"]
            ]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 ;; http/web
                 [compojure "1.1.1"]
                 [ring/ring-core "1.1.1"]
                 [ring/ring-servlet "1.1.1"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 [ring/ring-devel "1.1.1"]
                 [ring-json-params "0.1.3"]
                 [amalloy/ring-gzip-middleware "0.1.2"]

                 [clj-json "0.5.0"]
                 [aleph "0.2.2"]
                 
                 ;; logging
                 [org.clojure/tools.logging "0.2.4"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.0.7"]
                 [org.slf4j/log4j-over-slf4j "1.6.6"]

                 ;; database
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [com.h2database/h2 "1.3.168"]
                 [com.jolbox/bonecp "0.7.1.RELEASE"]
                 [korma "0.3.0-beta10" :exclusions [log4j/log4j]]
                 [lobos "1.0.0-SNAPSHOT"]
                 [org.clojure/core.cache "0.6.2"]

                 ;; remote repl
                 [org.clojure/tools.nrepl "0.2.0-beta9"]
                 [com.cemerick/pomegranate "0.0.13"]
                 
                 ;; misc
                 [joda-time/joda-time "2.1"]
                 [commons-codec/commons-codec "1.6"]
                 [org.clojure/tools.cli "0.2.2"]
                 ]
  :ring {:handler ruuvi-server.launcher/ring-handler,
         :init ruuvi-server.launcher/ring-init,
         :destroy ruuvi-server.launcher/ring-destroy}
  :profiles {:dev
             {:dependencies
              [
               [midje "1.4.0" :exclusions [org.clojure/clojure]]
               ]}}
  ;; enable OTF (on-the-fly compilation)
  :hooks [lein-otf.hooks]
  ;; Emit warnings on all reflection calls.
  ;;:warn-on-reflection true
  ;; AOT (ahead-of-time compilation) breaks migrations
  ;; :aot [ruuvi-server.launcher]
  :main ^:skip-aot ruuvi-server.launcher
  )
