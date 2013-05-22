(ns ruuvi-server.tracker-security-test
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

(def valid-password "joujou")
(def valid-password-params {:X-foobar "bar",
                            :tracker_code "foobar",
                            :longitude "02536.100084,E",
                            :password valid-password
                            :version "1",
                            :time "2012-04-02T18:35:11.000+0200",
                            :latitude "6457.934248,N"})

(def valid-shared-secret "foobar")
(def valid-tracker {:shared_secret valid-shared-secret})
(def valid-password-tracker {:password valid-password})
  
(fact "when tracker is not available, authentication is not attempted"
      (authentication-status valid-params nil :mac )
      => {:unknown-tracker true})

(fact "when mac or password field is not in message, authentication is not used"
      (authentication-status {:some "fields"} valid-tracker :mac)
      => {:not-authenticated true})

(fact "when computed mac does not match mac in message, authentication fails"
      (authentication-status valid-params {:shared_secret "wrong-shared-secret"} :mac)
      => {:authentication-failed true})

(fact "when request password does not match tracker's password, authentication fails"
      (authentication-status valid-password-params {:password "wrong-password"} :mac)
      => {:authentication-failed true})

(fact "when computed mac matches mac in message, authentication succeeds"
      (authentication-status valid-params valid-tracker :mac)
      => {:authenticated-tracker true}
 (provided (compute-hmac valid-params valid-shared-secret :mac) => valid-mac))

(fact "when request password matches trackers password, authentication succeeds"
      (authentication-status valid-password-params valid-password-tracker :mac)
      => {:authenticated-tracker true})

(fact
 (generate-mac-message valid-params :mac) => "X-foobar:bar|latitude:6457.934248,N|longitude:02536.100084,E|time:2012-04-02T18:35:11.000+0200|tracker_code:foobar|version:1|")

(fact
 (generate-mac-message valid-params :not-exists) => "X-foobar:bar|latitude:6457.934248,N|longitude:02536.100084,E|mac:17e4ccf60f766710d0695348d7fda63cee0a3d46|time:2012-04-02T18:35:11.000+0200|tracker_code:foobar|version:1|")

(fact "compute-hmac appends secret to end of (generate-mac-message ...) and computes HMAC-SHA1 from that"
 (compute-hmac valid-params "secret" :mac) => "fd264415979bb17f68a3e4fd3b645b7e763e3b56"
 (provided
  (generate-mac-message valid-params :mac) => "base"))
 
 
 
