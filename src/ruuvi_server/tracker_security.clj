(ns ruuvi-server.tracker-security
  (:import [java.security MessageDigest])
  (:import [javax.crypto Mac])
  (:import [javax.crypto.spec SecretKeySpec])
  (:import [org.apache.commons.codec.binary Hex])
  (:use [clojure.tools.logging :only (debug info warn error)]))

(defn generate-mac-message [params mac-field]
  (let [;; remove mac key
        non-mac-params (dissoc params mac-field)
        ;; sort keys alphabetically
        sorted-keys (sort (keys non-mac-params))
        ;; make included-keys a vector and convert to non-lazy list
        param-keys (vec sorted-keys)]
    ;; concatenate keys, values and separators
    (apply str (for [k param-keys]
                 (str (name k) ":" (params k) "|")))))
  
(defn compute-mac [params secret mac-field]
  (let [mac-message (generate-mac-message params mac-field)
        request-mac (params mac-field)]
    
    (let [value-with-shared-secret (str mac-message secret)
          messageDigester (MessageDigest/getInstance "SHA-1")]
      (let [computed-mac (.digest messageDigester (.getBytes value-with-shared-secret "ASCII"))
            computed-mac-hex (Hex/encodeHexString computed-mac)]
        (debug (str "orig-mac "(str request-mac) " computed mac " (str computed-mac-hex)) )
        computed-mac-hex
        ))))

(defn compute-hmac [params secret mac-field]
  (let [mac-message (generate-mac-message params mac-field)
        request-mac (params mac-field)
        algorithm "HmacSHA1"
        mac (Mac/getInstance algorithm)
        secret-key (SecretKeySpec. (.getBytes secret "ASCII") algorithm)]
    
    (.init mac secret-key)
    (let [computed-hmac (.doFinal mac (.getBytes mac-message "ASCII"))
          computed-hmac-hex (Hex/encodeHexString computed-hmac)]
      (debug (str "orig-hmac "(str request-mac) " computed hmac " (str computed-hmac-hex)) )
      computed-hmac-hex
      )))

(defn authentication-status
  "Sets keys

* `:authenticated-tracker`, if properly authenticated.
* `:not-authenticated`, if client chooses not to use autentication.
* `:unknown-tracker`, if client tracker is not known in database.
* `:authentication-failed`, autentication was attempted, but macs do not match."
  [params tracker mac-field]
  (let [request-mac (params mac-field)
        request-password (params :password)
        tracker-password (:password tracker)
        tracker-secret (:shared_secret tracker)]
    (cond
     (not (or request-mac request-password)) (do (debug "Client does not use authentication")
                                                 {:not-authenticated true})
     (not tracker) (do (debug "Tracker does not exist in system")
                       {:unknown-tracker true})
     
     (and request-mac (not tracker-secret)) (do (debug "No shared secret for tracker")
                                                {:authentication-failed true})

     (and request-password (not tracker-password)) (do (debug "No password for tracker")
                                                       {:authentication-failed true})

     (and request-mac tracker-secret)
     (let [computed-mac (compute-hmac params tracker-secret mac-field)]
       (if (= computed-mac request-mac)
         (do (debug "Tracker is authenticated successfully using shared secret")
             {:authenticated-tracker true})
         (do (debug "Tracker failed authentication using shared secret")
             {:authentication-failed true})
         ))
     
     (and request-password tracker-password)
     (if (= request-password tracker-password)
       (do (debug "Tracker is authenticated successfully using password")
           {:authenticated-tracker true})
       (do (debug "Tracker failed authentication using password")
           {:authentication-failed true})
       )
     
     :else (do (debug "Tracker failed authentication")
               {:authentication-failed true})
     )
    
    ))