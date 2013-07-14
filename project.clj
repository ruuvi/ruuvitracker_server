(defproject ruuvi-server/ruuvi-server "0.0.1-SNAPSHOT" 
  :description "RuuviTracker server"
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.8.6"]
            [lein-midje "3.0.1"]
            [lein-marginalia "0.7.1"]
            [org.clojars.llasram/lein-otf "2.1.2"]
            ]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; http/web
                 [compojure "1.1.5"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-servlet "1.2.0"]
                 [ring/ring-devel "1.2.0"]
                 [ring/ring-json "0.2.0"]
                 [amalloy/ring-gzip-middleware "0.1.2"]

                 [cheshire "5.2.0"]
                 [aleph "0.3.0-rc2"]

                 ;; validation
                 [org.clojars.runa/clj-schema "0.9.2"]

                 ;; security
                 [crypto-password "0.1.0"]

                 ;; logging
                 [org.clojure/tools.logging "0.2.6"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/log4j-over-slf4j "1.7.5"]

                 ;; external services
                 [clj-http "0.7.5"]
                 
                 ;; database
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [com.h2database/h2 "1.3.172"]
                 [com.jolbox/bonecp "0.7.1.RELEASE"]
                 [korma "0.3.0-RC5"]
                 [lobos "1.0.0-beta1"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]

                 ;; email
                 [clojurewerkz/mailer "1.0.0-alpha3"]

                 ;; remote repl
                 [org.clojure/tools.nrepl "0.2.3"]
                 ;;[com.cemerick/pomegranate "0.0.13"]
                 
                 ;; misc
                 [clj-time "0.5.1"]
                 [commons-codec/commons-codec "1.7"]
                 [org.clojure/tools.cli "0.2.2"]
                 ;; TODO lein 0.2.0-RC2 requires this
                 [bultitude "0.2.2"]
                 ]
  :ring {:handler ruuvi-server.launcher/ring-handler,
         :init ruuvi-server.launcher/ring-init,
         :destroy ruuvi-server.launcher/ring-destroy}

  :profiles {:dev
             {:dependencies
              [
               [midje "1.5.1" :exclusions [org.clojure/clojure]]
               ]}}

  :jvm-opts ["-server" "-XX:+UseConcMarkSweepGC"]

  ;; enable OTF (on-the-fly compilation)
  ;; :hooks [lein-otf.hooks]
  
  ;; Emit warnings on all reflection calls.
  ;;:warn-on-reflection true

  ;; AOT (ahead-of-time compilation). Breaks migrations and midje tests
  ;; :aot [ruuvi-server.launcher]

  :main ruuvi-server.launcher
  )
