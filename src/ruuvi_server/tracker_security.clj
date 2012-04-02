(ns ruuvi-server.tracker-security
  (:import [java.security MessageDigest])
  (:import [org.apache.commons.codec.binary Hex])
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn- compute-mac [params secret mac-field]
  (let [request-mac (params mac-field)
        ; sort keys alphabetically
        sorted-keys (sort (keys params))
        ; remove mac key
        included-keys (filter (fn [param-key]
                                (not= mac-field param-key))
                              sorted-keys)
        ; make included-keys a vector and convert to non-lazy list
        param-keys (vec included-keys)]
    
   ; concatenate keys, values and separators. also add shared secret
   (let [value (str (apply str (for [k param-keys]
                                  (str (name k) ":" (params k) "|")
                                  )))
         value-with-shared-secret (str value secret)
         messageDigester (MessageDigest/getInstance "SHA-1")]
      (let [computed-mac (.digest messageDigester (.getBytes value-with-shared-secret "ASCII"))
            computed-mac-hex (Hex/encodeHexString computed-mac)]
        (debug (str "orig-mac "(str request-mac) " computed mac " (str computed-mac-hex)) )
        computed-mac-hex
        ))))


(defn authentication-status
"Sets keys
- :authenticated-tracker, if properly authenticated.
- :not-authenticated, if client chooses not to use autentication.
- :unknown-tracker, if client tracker is not known in database.
- :authentication-failed, autentication was attempted, but macs do not match."
  [params tracker mac-field]
  (cond
   (not (params mac-field)) (do (debug "Client does not use authentication")
                                {:not-authenticated true})
   
   (not tracker) (do (debug "Tracker does not exist in system")
                     {:unknown-tracker true})
   
   :else (let [shared-secret (tracker :shared_secret)
               computed-mac (compute-mac params shared-secret mac-field)
               request-mac (params mac-field)]
           (if (= computed-mac request-mac)
             (do (debug "Tracker is authenticated successfully")
                 {:authenticated-tracker true})
             (do (debug "Tracker failed authentication")
                 {:authentication-failed true})
             ))))
