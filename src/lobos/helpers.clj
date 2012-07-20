(ns lobos.helpers
  (:use lobos.connectivity)
  (:refer-clojure :exclude [bigint boolean char double float time])
  (:use lobos.schema )
  )

(defn surrogate-key [table]
  (integer table :id :auto-inc :primary-key))

(defn timestamps [table]
  (-> table
      (timestamp :updated_on :not-null (default (now)))
      (timestamp :created_on :not-null (default (now)))))

(defn refer-to [table ptable]
  (let [cname (-> (->> ptable name butlast (apply str))
                  (str "_id")
                  keyword)]
    (integer table cname :not-null [:refer ptable :id :on-delete :cascade])))

(defmacro table-entity [name & elements]
  `(-> (table ~name)
       (timestamps)
       ~@(reverse elements)
       (surrogate-key)))

