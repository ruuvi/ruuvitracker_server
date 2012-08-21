(ns ruuvi-server.test.tracker-security
  (:use midje.sweet
        ruuvi-server.tracker-security)
  )

(def valid-mac "17e4ccf60f766710d0695348d7fda63cee0a3d46")
(def valid-params {:X-foobar "bar",
                   :tracker_code "foobar",
                   :longitude "02536.100084,E",
                   :mac valid-mac
                   :version "1",
                   :time "2012-04-02T18:35:11.000+0200",
                   :latitude "6457.934248,N"})

(def valid-shared-secret "foobar")
(def valid-tracker {:shared_secret valid-shared-secret})
  
(fact "when tracker is not available, authentication is not attempted"
 (authentication-status valid-params nil :mac )
 => {:unknown-tracker true})

(fact "when mac field is not in message, authentication is not used"
 (authentication-status {:some "fields"} valid-tracker :mac)
 => {:not-authenticated true})

(fact "when computed mac does not match mac in message, authentication fails"
 (authentication-status valid-params {:shared_secret "wrong-shared-secret"} :mac)
 => {:authentication-failed true})

(fact "when computed mac matches mac in message, authentication succeeds"
 (authentication-status valid-params valid-tracker :mac)
 => {:authenticated-tracker true}
 (provided (compute-hmac valid-params valid-shared-secret :mac) => valid-mac))


(fact
 (generate-mac-message valid-params :mac) => "X-foobar:bar|latitude:6457.934248,N|longitude:02536.100084,E|time:2012-04-02T18:35:11.000+0200|tracker_code:foobar|version:1|")

(fact
 (generate-mac-message valid-params :not-exists) => "X-foobar:bar|latitude:6457.934248,N|longitude:02536.100084,E|mac:17e4ccf60f766710d0695348d7fda63cee0a3d46|time:2012-04-02T18:35:11.000+0200|tracker_code:foobar|version:1|")

(fact "compute-mac appends secret to end of (generate-mac-message ...) and computes SHA1 from that"
 (compute-mac valid-params "secret" :mac) => "f9cc37a35218a85cb8871e9812311fd29e7e25b8"
 (provided
  (generate-mac-message valid-params :mac) => "base"))
 
(fact "compute-hmac appends secret to end of (generate-mac-message ...) and computes HMAC-SHA1 from that"
 (compute-hmac valid-params "secret" :mac) => "fd264415979bb17f68a3e4fd3b645b7e763e3b56"
 (provided
  (generate-mac-message valid-params :mac) => "base"))
 
 
 