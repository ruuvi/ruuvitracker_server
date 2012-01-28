(defproject ruuvi-server "0.0.1-SNAPSHOT"
  :description "RuuviTracker server"
  :dependencies
  [
   [clj-stacktrace/clj-stacktrace "0.2.4"]
   [org.clojure/clojure "1.2.1"]
   [org.clojure/clojure-contrib "1.2.0"]
   [compojure "1.0.1"]
   [ring-json-params "0.1.3"]
   [clj-json "0.5.0"]
   [ring "1.0.1" :exclusions [clj-stacktrace/clj-stacktrace]]
   [ring/ring-servlet "1.0.1"]
   [ring/ring-jetty-adapter "1.0.1"]
   ; java deps
   [joda-time/joda-time "2.0"]
   [ch.qos.logback/logback-classic "1.0.0"]
   ]
  :dev-dependencies
  [[ring/ring-devel "1.0.1"]
   [lein-ring "0.5.4"]
   ]
  :ring {:handler ruuvi-tracker.core/dev-app}
  )


