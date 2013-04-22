(ns ruuvi-server.launcher
  "Starting point of RuuviTracker server. Parses command line parameters, executes migrations and starts server."
  (:require [clojure.tools.cli :as cli]
            [ruuvi-server.configuration :as conf]
            [aleph.http :as aleph]
            [ruuvi-server.core :as ruuvi-server]
            [ruuvi-server.database.entities :as entities]
            [lobos.migrations :as migrations]
            [ruuvi-server.database.load-example-data :as load-example-data]
            [clojure.tools.nrepl.server :as nrepl]
            )
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:gen-class)
  )

(defn- parse-env [value]
  (when-not (contains? #{"dev" "prod"} value)
    (throw (IllegalArgumentException. "server must be either 'dev' or 'prod'")))
  (keyword value))

(defn- parse-command-line-args [args]
  (cli/cli args
           ["-p" "--port" "Port to listen." :parse-fn #(Integer/valueOf %)]
           ["-c" "--config" "Configuration file."]
           ["-P" "--platform" "Platform. Either 'standalone' or 'heroku'."]
           ["-e" "--env" "Environment. Either 'dev' or 'prod'." :parse-fn parse-env]
           ["-R" "--remote-repl-port" "Start remote REPL in given port. Allows connections only from localhost." :parse-fn #(Integer/valueOf %)]
           ["-v" "--version" "Version information." :default false :flag true]
           ["-h" "--help" "Help" :default false :flag true]
           )
  )

(defn- display-end-text [text]
  (println text)
  (System/exit 0))

(defn- display-version []
  (display-end-text "RuuviServer 0.1"))

(def ^:private remote-repl-server (atom nil))

(defn- start-repl-server
  "Starts remote repl in given configured port. Binds only to localhost."
  [config]
  (let [port (:port (:remote-repl config))
        bind-address "127.0.0.1"]
    (when port
      (info "Starting remote REPL in port" port)
      (reset! remote-repl-server (nrepl/start-server :port port :bind bind-address)))))

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
  (info "Reading configuration from" config-file)
  (info "Overriding configuration file with parameters" params)
  (let [config (conf/read-config config-file)
        config (update-in config [:environment] #(or (:env params) % :prod))
        config (update-in config [:server :type] #(or (:platform params) % :standalone))
        config (update-in config [:server :port] #(or (:port params) % 8080))
        config (update-in config [:remote-repl :port] #(or (:remote-repl-port params) %))
        config (conf/post-process-config (:type (:server config)) config)]

    config
  ))

(defn- load-config [params]
  (let [config-file (get-config-file params)
        config (read-config-file config-file params)
        ]
    (let [printable-config (update-in config [:database :password] (fn [a] "********"))]
      (info "Using configuration" printable-config))
    (start-repl-server config)
    (conf/init-config config)
    (entities/init config)
    config))

(defn- create-ring-handler [config]
  (let [env (:environment config)] 
    (cond (= env :prod) ruuvi-server/application-prod
          (= env :dev) ruuvi-server/application-dev
          :default (throw (IllegalArgumentException. (str "Illegal environment" env ". Must be :prod or :dev.")))))
  )

(defn- start-aleph-server [config port]
  (let [handler (aleph/wrap-ring-handler (create-ring-handler config))]
    (info "Starting remote Aleph server in port" port)
    (aleph/start-http-server handler {:port port :websocket (get-in config [:server :websocket])})))

(defn start-server [config & args]
  (let [{:keys [environment server]} config
        {:keys [port]} server]
    (start-aleph-server config port)))

(defn migrate [config args]
  (migrations/do-migration config (keyword (or (first args) :forward))))

(defn- load-example-data [config args]
  (load-example-data/create-test-trackers))

(defn- parse-command [values]
  (let [value (keyword (first values))]
        (cond (not value) [:server]
              :default (if (contains? #{:server :migrate :load-example-data} value)
                         (into [value] (rest values))
                         (throw (IllegalArgumentException. "Command must be one of the 'server', 'migrate' or 'load-example-data'."))))))

(defn- register-shutdown-hook []
  (.addShutdownHook
   (Runtime/getRuntime)
   (proxy [Thread] []
     (run []
       (info "Server shutting down")))))

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
              (register-shutdown-hook)
              (println "RuuviServer starting")
              (start-server config args))

            (= command :migrate)
            (let []
              (println "Doing migration")
              (migrate config args))

            (= command :load-example-data)
            (let []
              (println "Load example data to database")
              (load-example-data config args))
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
