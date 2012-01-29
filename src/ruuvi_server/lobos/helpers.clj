(ns ruuvi-server.lobos.helpers
  (:use lobos.connectivity)
  (:refer-clojure :exclude [bigint boolean char double float time])
  (:use lobos.schema )
  )

(defn surrogate-key [table]
  (integer table :id :auto-inc :primary-key))

(defn timestamps [table]
  (-> table
      (timestamp :updated_on)
      (timestamp :created_on (default (now)))))

(defn refer-to [table ptable]
  (let [cname (-> (->> ptable name butlast (apply str))
                  (str "_id")
                  keyword)]
    (integer table cname [:refer ptable :id :on-delete :set-null])))


(defmacro tbl [name & elements]
  `(-> (table ~name
              (surrogate-key)
              (timestamps))
       ~@elements))

