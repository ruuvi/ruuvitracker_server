(ns ruuvi-server.database.load-initial-data
  (:use [ruuvi-server.database.event-dao :only (create-tracker)]
        [korma.db :only (transaction)]
        [clojure.tools.logging :only (debug info warn error)])
  )

(defn create-test-trackers []
  (info "Creating test trackers")
  (transaction 
   (create-tracker "990123" "test-tracker" "salakala")
   (create-tracker "123" "Murre-tracker" "password")
   (create-tracker "foo" "FooBar" "bar")
   (create-tracker "foobar" "foobar" "foobar")
   )
)
