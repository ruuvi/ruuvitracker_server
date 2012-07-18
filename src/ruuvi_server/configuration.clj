(ns ruuvi-server.configuration
  (:require [clojure.java.io :as io])
  (:import [java.io PushbackReader])
  (:import java.net.URI)
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn read-config-with-eval
  "Reads a configuration file in clojure format from a file or classpath.
Executable code is allowed."
  [file]
  (with-open [stream (or (try (io/reader file)
                              (catch Exception e (io/reader (io/resource file)))))
              ]
    (read (PushbackReader. stream))))

(defn read-config
  "Reads a configuration file in clojure format from a file or classpath.
Executable code is not allowed."
  [file]
  (binding [*read-eval* false]
    (read-config-with-eval file)))


(defn- heroku-database-config []
    ;; Heroku DATABASE_URL looks like this:
    ;; postgres://username:password@some.host.at.amazonaws.com/databasename
    (let [uri (URI. (System/getenv "DATABASE_URL"))
          splitted-userinfo (.split (.getUserInfo uri) ":")]
      {:classname "org.postgresql.Driver"
       :subprotocol "postgresql"
       :user (nth splitted-userinfo 0)
       :password (nth splitted-userinfo 1)
       :subname (str "//" (.getHost uri) (.getPath uri))
       }))

(defn- post-process-config [env conf]
  (if (= (:type (conf :server)) :heroku)
    (merge conf
           {:database (heroku-database-config)
            :server (merge (conf :server)
                           :port (System/getenv "PORT")
                           )})
    conf)
  )


(defn create-config []
  (let [property-name "RUUVISERVER_ENV"
        env (or (System/getProperty property-name) (System/getenv property-name) "dev")
        config-file (str "server-" env "-config.clj")
        conf (read-config-with-eval config-file)
        processed-config (post-process-config (keyword env) conf)]
    (info "Using configuration" conf)
    processed-config
  ))

;; This creates the configuration object automatically
;; when this namespace is imported.

;; TODO maybe config should be created on-demand?
(def ^:dynamic *config* (create-config))
