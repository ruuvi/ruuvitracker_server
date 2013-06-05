(ns ruuvi-server.configuration
  (:require [clojure.java.io :as io]
            [ruuvi-server.database.pool :as pool]
            [clojure.tools.reader.edn :as edn] )
  (:use [clojure.tools.logging :only (debug info warn error)] )
  (:import [java.io PushbackReader]
           java.net.URI) )

(defn read-config
  "Reads a configuration file in clojure format from a file or classpath.
Executable code is not allowed."
  [file]
  (with-open [stream (try (io/reader file)
                          (catch Exception e (io/reader (io/resource file)))) ]
    (edn/read (PushbackReader. stream))))

(defn- heroku-database-config []
    ;; Heroku DATABASE_URL looks like this:
    ;; postgres://username:password@some.host.at.amazonaws.com/databasename
    (let [uri (URI. (System/getenv "DATABASE_URL"))
          splitted-userinfo (.split (.getUserInfo uri) ":")]
      {:classname "org.postgresql.Driver"
       :subprotocol "postgresql"
       :user (nth splitted-userinfo 0)
       :password (nth splitted-userinfo 1)
       :subname (str "//" (.getHost uri) (.getPath uri)) } ))

(defn post-process-config [env conf]
  (let [config
        (if (= (:type (conf :server)) :heroku)
          (merge conf
                 {:database (heroku-database-config)
                  :server (merge (conf :server)
                                 {:port (Integer. (or (System/getenv "PORT") 5000))})})
          conf)
        config (update-in config [:database :datasource] (fn [_] (pool/create-connection-pool (:database config))))]
    config ))

(defn create-config []
  (let [property-name "RUUVISERVER_ENV"
        env (or (System/getProperty property-name) (System/getenv property-name) "dev")
        config-file (str "server-" env "-config.clj")
        conf (read-config config-file)
        processed-config (post-process-config (keyword env) conf)]
    (let [db-conf (:database conf)
          safe-db-conf (merge db-conf {:password "********"})
          safe-conf (merge conf {:database safe-db-conf}) ]
      (info "Using configuration" safe-conf) )
    processed-config ))

;; This creates the configuration object automatically
;; when this namespace is imported.
(def ^:private internal-config (atom nil))

(defn get-config [] @internal-config)

(defn init-config
  ([] (reset! internal-config (create-config)))
  ([config] (reset! internal-config config)))
