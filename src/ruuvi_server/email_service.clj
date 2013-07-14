(ns ruuvi-server.email-service
  (:require [clojurewerkz.mailer.core 
             :refer [with-delivery-mode deliver-email]]))

;; email templates are defined in resources/templates/email directory
;; templates use http://mustache.github.io/ as markup language

(defn send-registration! [config email name username]
  (with-delivery-mode (or (-> config :email :delivery-mode) :test)
    (deliver-email {:from "RuuviTracker@gmail.com" 
                    :to [email] 
                    :subject "RuuviTracker.fi registration"}
                   "templates/email/registration.mustache" 
                   {:name name :username username})))

