(ns ruuvi-server.load-initial-data
  (:use ruuvi-server [database common])
  (:use ruuvi-server.models.entities)
  )

(defn create-test-trackers []
  (print "Creting test trackers")
  (create-tracker "990123" "test-tracker" "salakala")
  (create-tracker "123" "Murre-tracker" "password")
  (create-tracker "foo" "FooBar" "bar")
  (create-tracker "foobar" "foobar" "foobar")
)