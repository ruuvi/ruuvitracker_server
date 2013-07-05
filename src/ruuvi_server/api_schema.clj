(ns ruuvi-server.api-schema
  "Schema validation for incoming API data."
  (:require [clj-schema.schema :refer [def-map-schema optional-path sequence-of 
                                       map-schema set-of
                                       constraints]]
            [clj-schema.validation :refer [validation-errors]]
            [ruuvi-server.parse :refer [parse-decimal parse-coordinate parse-timestamp parse-integer]]
            ))

(defn- max-length [len] (fn [str] (<= (count str) len)))
(defn- min-length [len] (fn [str] (>= (count str) len)))
(def ^{:private true} name? [String (max-length 256)])
(def ^{:private true} code? [String (min-length 4) (max-length 30) 
                             #"^[a-zA-Z0-9]+$"])
(def ^{:private true} shared-secret? [String (min-length 4) (max-length 30)
                                      #"^[a-zA-Z0-9]+$"])
(def ^{:private true} email? [String 
                              (fn [val] (count val) <  2)
                              #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$"])
(def ^{:private true} password? [String (max-length 50)])
(def ^{:private true} text? [String])

(defn- password-or-shared-secret? [m] 
  (or (-> m :tracker :password)
      (-> m :tracker :shared_secret)))

(def-map-schema :loose new-group-schema
  [[:group :name] [String (max-length 256)]])

(def-map-schema :loose new-user-schema
  [[:user :username] email?
   [:user :password] password?])

(def-map-schema authenticate-schema 
  [[:user :username] email?
   [:user :password] password?])

(def-map-schema :loose new-tracker-schema
  (constraints password-or-shared-secret?)
  [[:tracker :name] name?
   [:tracker :code] code?
   (optional-path [:tracker :shared_secret]) shared-secret?
   (optional-path [:tracker :password]) password?
   (optional-path [:tracker :description]) [text? (max-length 256)]
   ])

(def ^{:private true} timestamp? parse-timestamp)
(def ^{:private true} latitude? parse-coordinate)
(def ^{:private true} longitude? parse-coordinate)
(def mac? [String #"^[a-fA-F0-9]{40}$"])
(defn- valid-decimal? [x] (parse-decimal x))
(defn- positive-decimal? [x] (not (neg? (parse-decimal x))))
(defn- positive-integer? [x] (not (neg? (parse-integer x))))

(defn- extension-values-valid? 
  "Checks that X- keys or values are not too long."
  [x] 
  (let [exts (map (fn [item] [(name (first item)) (str (last item))]) x)]
     (empty? (filter (fn [item]
                      (let [key (first item)]
                        (and (.startsWith (.toLowerCase key) "x-")
                             (or (> (count key) 256) 
                                 (> (count (last item)) 256) )))) 
                     exts))))

(def-map-schema :loose new-single-event-schema
  (constraints extension-values-valid?)
  [[:version] 1
   [:tracker_code] code?
   (optional-path [:time]) timestamp?
   (optional-path [:session_code]) [String (max-length 256)]
   (optional-path [:nonce]) String
   (optional-path [:latitude]) latitude?
   (optional-path [:longitude]) longitude?
   (optional-path [:accuracy]) positive-decimal?
   (optional-path [:vertical_accuracy]) positive-decimal?
   (optional-path [:heading]) positive-decimal?
   (optional-path [:satellite_count]) positive-integer?
   (optional-path [:battery]) positive-decimal?
   (optional-path [:speed]) positive-decimal?
   (optional-path [:altitude]) valid-decimal?
   (optional-path [:temperature]) valid-decimal?
   (optional-path [:annotation]) [text? (max-length 256)]
   (optional-path [:mac]) mac? ])
