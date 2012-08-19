(ns ruuvi-server.launcher
  (:require [clojure.tools.cli :as cli]
            [ruuvi-server.configuration :as conf]
            [ring.adapter.jetty :as jetty]
            [ruuvi-server.core :as ruuvi-server]
            [ruuvi-server.database.entities :as entities]
            [lobos.migrations :as migrations]
            [ruuvi-server.database.load-initial-data :as load-initial-data]
            )
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:gen-class))

(defn- parse-server [value]
  (when-not (contains? #{"aleph" "jetty"} value)
    (throw (IllegalArgumentException. "server must be either 'jetty' or 'aleph'")))
  (keyword value))

(defn- parse-env [value]
  (when-not (contains? #{"dev" "prod"} value)
    (throw (IllegalArgumentException. "server must be either 'dev' or 'prod'")))
  (keyword value))

(defn- parse-command-line-args [args]
  (cli/cli args
           ["-p" "--port" "Port to listen." :parse-fn #(Integer/valueOf %)]
           ["-c" "--config" "Configuration file."]
           ["-s" "--engine" "Server type. Either 'jetty' or 'aleph'." :parse-fn parse-server]
           ["-P" "--platform" "Platform. Either 'standalone' or 'heroku'."]
           ["-e" "--env" "Environment. Either 'dev' or 'prod'." :parse-fn parse-env]
           ["-v" "--version" "Version information." :default false :flag true]
           ["-h" "--help" "Help" :default false :flag true]
           )
  )

(defn- display-end-text [text]
  (println text)
  (System/exit 0))

(defn- display-version []
  (display-end-text "RuuviServer 0.1"))

(defn- get-config-file [params]
  (let [config-file (:config params)]
    (if config-file
      config-file
      (let [property-name "RUUVISERVER_ENV"
            env (or (System/getProperty property-name) (System/getenv property-name) "dev")
            config-file (str "server-" env "-config.clj")]
        config-file))
  ))

(defn- read-config-file [config-file params]
  (let [config (conf/read-config config-file)
        config (update-in config [:environment] #(or (:env params) % :prod))
        config (update-in config [:server :type] #(or (:platform params) % :standalone))
        config (update-in config [:server :port] #(or (:port params) % 8080))
        config (update-in config [:server :engine] #(or (:engine params) % :jetty))
        config (conf/post-process-config (:type (:server config)) config)]
    config
  ))

(defn- load-config [params]
  (let [config-file (get-config-file params)
        config (read-config-file config-file params)
        ]
    (let [printable-config (update-in config [:database :password] (fn [a] "********"))]
      (info "Using configuration" printable-config))
    (conf/init-config config)
    (entities/init)
    config))

(defn- create-ring-handler [config]
  (let [env (:environment config)] 
    (cond (= env :prod) ruuvi-server/application-prod
          (= env :dev) ruuvi-server/application-dev
          :default (throw (IllegalArgumentException. (str "Illegal environment" env ". Must be :prod or :dev.")))))
  )

(defn- start-jetty-server [config port max-threads]
  (jetty/run-jetty (create-ring-handler config) {:port port :join? false :max-threads max-threads}))

(defn- start-aleph-server [config]
  (throw (IllegalArgumentException. "Aleph server not yet supported"))
  ;;(start-http-server (wrap-ring-handler (create-ring-handler params) ) )
  )

(defn- start-server [config]
  (let [{:keys [environment server]} config
        {:keys [port engine max-threads]} server
        ]
    (cond (= engine :jetty) (start-jetty-server config port max-threads)
          (= engine :aleph) (start-aleph-server config)
          :default (throw (IllegalArgumentException. (str "Unsupported server engine " engine ". Supported 'jetty' and 'aleph'."))))))

(defn- migrate [config args]
  (migrations/do-migration (keyword (or (first args) :forward))))

(defn- load-test-data [config args]
  (load-initial-data/create-test-trackers))

(defn- parse-command [values]
  (let [value (keyword (first values))]
        (cond (not value) :server
              :default (if (contains? #{:server :migrate :load-test-data} value)
                         (into [value] (rest values))
                         (throw (IllegalArgumentException. "Command must be one of the 'server', 'migrate' or 'load-test-data'."))))))

(defn -main [& args]
  (let [parsed (parse-command-line-args args)
        [params commands-arg help] parsed]
    (cond (:help params) (display-end-text help)
          (:version params) (display-version))

    (let [[command & args] (parse-command commands-arg)
          config (load-config params)]
      ;; TODO replace with multimethod
      (cond (= command :server)
            (let []
              (println "RuuviServer starting")
              (start-server config args))

            (= command :migrate)
            (let []
              (println "Doing migration")
              (migrate config args))

            (= command :load-test-data)
            (let []
              (println "Load test data to database")
              (load-test-data config args))
            )
      )))

(def ^:private global-ring-handler (atom nil))
(defn ring-init []
  (info "Initializing ring")
  (let [config (load-config nil)]
    (reset! global-ring-handler (create-ring-handler config)))
  )

(defn ring-destroy []
  (info "Finishing ring"))

(defn ring-handler [req]
  (@global-ring-handler req))
