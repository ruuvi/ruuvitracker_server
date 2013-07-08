(ns ruuvi-server.database.load-example-data
  (:use [ruuvi-server.database.event-dao :only (create-tracker)]
        [korma.db :only (transaction)]
        [clojure.tools.logging :only (debug info warn error)])
  )

(defn create-test-trackers []
  (info "Creating test trackers")
  (transaction 
   (create-tracker nil "990123" "test-tracker" "salakala" "salakala" nil true)
   (create-tracker nil "123" "Murre-tracker" "password" "password" "Murren mukana aina" true)
   (create-tracker nil "foo" "FooBar" "bar" "password" "baz quux" true)
   (create-tracker nil "foobar" "foobar" "foobar" "foobar" "foobar" true)
   )
)
