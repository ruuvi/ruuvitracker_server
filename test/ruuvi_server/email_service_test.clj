(ns ruuvi-server.email-service-test
  (:require [ruuvi-server.email-service :refer [send-registration!]]
            [clojurewerkz.mailer.core :refer [delivery-mode!]] )
  (:use midje.sweet))

(def config {:email {:delivery-mode :test}})
(fact (send-registration! config "test@example.com" "John Smith" "test-example@example.com") => truthy)
