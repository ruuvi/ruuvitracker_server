(ns ruuvi-server.database.load-example-data
  (:use [ruuvi-server.database.event-dao :only (create-tracker)]
        [korma.db :only (transaction)]
        [clojure.tools.logging :only (debug info warn error)])
  )

(defn create-test-trackers []
  (info "Creating test trackers")
  (transaction 
   (create-tracker "990123" "test-tracker" "salakala" "salakala" nil)
   (create-tracker "123" "Murre-tracker" "password" "password" "Murren mukana aina")
   (create-tracker "foo" "FooBar" "bar" "password" "baz quux")
   (create-tracker "foobar" "foobar" "foobar" "foobar" "foobar")
   )
)
