(ns ruuvi-server.parse
  (:require [clojure.string :as string])
  )

;; TODO handle required fields
;; {:field {:parser func :required true}}
(defn parse-parameters
  "Params is map fieldname-keyword -> string.
Handlers is a map fieldname-keyword -> {:parser function :required boolean}. Parse
function returns parsed object or throws exception
Returns a map {field-name-keyword {:parsed-value <Value as parsed object}}
or {field-name-keyword {:error 'Error message'}
 {:time {:parsed-value <DateTime>} :latitude {:error 'ABC is not valid latitude value'}}
"
  [params parsers]
  (let [parsed-values
        (for [[field {parser :parser required :required}] parsers]
          (cond
           (and required (string/blank? (params field)))
           [field {:error (str "Field " field " is required.")}]
           
           (contains? params field)
           (try 
             [field {:parsed-value (parser (params field))}]
             (catch Exception e
               [field {:error (.getMessage e)}]
               ))
           
           :default nil))
        ]
    (into {} parsed-values) 
    )
)

(defn is-valid? [parsed-values]
  (let [errors (filter
                (fn [[field data]] (data :error))                          
                parsed-values)]
    (empty? errors)))

(defn get-value [parsed-values field]
  ((parsed-values field) :parsed-value))

(defn get-error [parsed-values field]
  ((parsed-values field) :error))
  