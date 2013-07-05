(ns ruuvi-server.api-schema-test
  (:require [clj-schema.validation :refer [validation-errors]]
            [ruuvi-server.util :refer [dissoc-in]] )
  (:use [ruuvi-server.api-schema]
        [midje.sweet]))

(defn- str-len [value len]
  (reduce str (take len (repeat value))))

;; trackers
(def tracker-full {:tracker {:name "tracker1"
                             :code "xyzzy"
                             :shared_secret "abczyx"
                             :password "secret"
                             :description "important tracker"
                             :extra-data 1}})
(def tracker-no-name (dissoc-in tracker-full [:tracker :name]))
(def tracker-password (dissoc-in tracker-full [:tracker :shared_secret]))
(def tracker-shared-secret (dissoc-in tracker-full [:tracker :password]))
(def tracker-no-auth (dissoc-in tracker-password [:tracker :password]))

(fact (validation-errors new-tracker-schema tracker-full) => #{}
      (validation-errors new-tracker-schema tracker-no-name) => #{"Map did not contain expected path [:tracker :name]."}
      (validation-errors new-tracker-schema tracker-password) => #{}
      (validation-errors new-tracker-schema tracker-shared-secret) => #{}
      (validation-errors new-tracker-schema tracker-no-auth) => #{"Constraint failed: 'password-or-shared-secret?'"})

;; users
(def user-full {:user {:username "pekka@example.com"
                       :password "verysecret"
                       :extra-data 1}})

(def user-invalid-email (assoc-in user-full [:user :username] "foo" ))

(def user-too-long-password (assoc-in user-full [:user :username] (str-len "x" 31) ))

(fact (validation-errors new-user-schema user-full) => empty?
      (validation-errors new-user-schema user-invalid-email) => (contains #{truthy})
      (validation-errors new-user-schema user-too-long-password) => (contains #{truthy})
      )

;; user authentication
(def user-auth {:user {:username "pekka@example.com"
                       :password "verysecret"}})

(fact (validation-errors authenticate-schema user-auth) => empty?
      (validation-errors authenticate-schema user-full) => (contains #{truthy}))

;; groups
(def group-full {:group {:name "a-group"}})
(def group-too-long-name (assoc-in group-full [:group :name] (str-len "x" 257)))
(fact (validation-errors new-group-schema group-full) => empty?
      (validation-errors new-group-schema group-too-long-name) => (contains #{truthy}))

;; events
(def event-minimal {:version 1
                    :tracker_code "abcd"})

(def event-full {:version 1
                 :tracker_code "abcd"
                 :time "1373053462"
                 :session_code "abc"
                 :nonce "abc"
                 :latitude "60.1708"
                 :longitude "2456.15,E"
                 :accuracy "1.2"
                 :vertical_accuracy "3.14"
                 :heading "231.0"
                 :satellite_count "1"
                 :battery "99.0"
                 :altitude "30.10"
                 :temperature "21.5"
                 :annotation "some important location"
                 :mac "CAFEbabeDE123456789012345678901234567890"
                 :extra-data 1
                 :X-bar "1234"
                 :x-foo 23212 } )

(def event-too-long-extension-key (assoc-in event-minimal [(keyword (str "X-" (str-len "x" 257)))] "1"))
(def event-too-long-extension-value (assoc-in event-minimal [:X-quux] (str-len "x" 257)) )

(fact (validation-errors new-single-event-schema event-minimal) => empty?
      (validation-errors new-single-event-schema event-full) => empty?
      (validation-errors new-single-event-schema event-too-long-extension-key) => (contains #{truthy})
      (validation-errors new-single-event-schema event-too-long-extension-value) => (contains #{truthy})
)
