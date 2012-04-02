(ns ruuvi-server.test.tracker-security
  (:use midje.sweet)
  (:use ruuvi-server.tracker-security)
  )

(def valid-params {:X-foobar "bar",
                   :tracker_code "foobar",
                   :longitude "02536.100084,E",
                   :mac "17e4ccf60f766710d0695348d7fda63cee0a3d46",
                   :version "1",
                   :time "2012-04-02T18:35:11.000+0200",
                   :latitude "6457.934248,N"})

(def valid-tracker {:shared_secret "foobar"})
  
(fact
 (authentication-status valid-params nil :mac )
 => {:unknown-tracker true})

(fact
 (authentication-status {:some "fields"} valid-tracker :mac)
 => {:not-authenticated true})

(fact
 (authentication-status valid-params {:shared_secret "wrong-shared-secret"} :mac)
 => {:authentication-failed true})

(fact
 (authentication-status valid-params valid-tracker :mac)
 => {:authenticated-tracker true})
