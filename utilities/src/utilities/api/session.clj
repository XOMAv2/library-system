(ns utilities.api.session
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request make-cb]])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol SessionAPI
  (-add-user [this user] "")
  (-get-user [this access-token uid] "")
  (-get-all-users [this access-token] "")
  (-update-user [this access-token uid user] "")
  (-delete-user [this access-token uid] "")
  (-get-tokens [this email password] "")
  (-refresh-tokens [this refresh-token] "")
  (-verify-token [this access-token] ""))

(defrecord SessionService [uri ^CircuitBreaker cb]
  SessionAPI
  (-add-user [this user]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/users")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str user)}))
  (-get-user [this access-token uid]
    (cb-sync-request cb {:method :get
                         :url (str (remove-trailing-slash uri) "/api/users/" uid)
                         :headers {"Authorization" (str "Bearer " access-token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-get-all-users [this access-token]
    (cb-sync-request cb {:method :get
                         :url (str (remove-trailing-slash uri) "/api/users")
                         :headers {"Authorization" (str "Bearer " access-token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-update-user [this access-token uid user]
    (cb-sync-request cb {:method :patch
                         :url (str (remove-trailing-slash uri) "/api/users/" uid)
                         :headers {"Authorization" (str "Bearer " access-token)
                                   "Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str user)}))
  (-delete-user [this access-token uid]
    (cb-sync-request cb {:method :delete
                         :url (str (remove-trailing-slash uri) "/api/users/" uid)
                         :headers {"Authorization" (str "Bearer " access-token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-get-tokens [this email password]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/login")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:email email
                                     :password password})}))
  (-refresh-tokens [this refresh-token]
    (cb-sync-request cb {:method :put
                         :url (str (remove-trailing-slash uri) "/api/auth/refresh")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:refresh-token refresh-token})}))
  (-verify-token [this access-token]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/verify")
                         :headers {"Content-type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:access-token access-token})})))

(defn make-session-service [uri cb-options]
  (->SessionService uri (make-cb cb-options)))

(comment
  (def session-service
    (make-session-service "http://127.0.0.1:3005"
                          {:failure-threshold-ratio [3 6]
                           :delay-ms 10000}))

  (require '[diehard.circuit-breaker])

  (let [resp (-get-tokens session-service "admin@admin.com" "admin")
        state (diehard.circuit-breaker/state (:cb session-service))]
    [state resp])

  (-verify-token session-service "access-token")

  (-refresh-tokens session-service "access-token")

  (-get-all-users session-service "access-token")

  (-add-user session-service {:name "Nikki"
                              :email "Nikki"
                              :role "admin"
                              :password "Nikki"})

  (-get-user session-service "access-token"
             #uuid "51a063a3-488d-4ece-86d8-83d679c28d52")

  (-update-user session-service "access-token"
                #uuid "51a063a3-488d-4ece-86d8-83d679c28d52" {:name "Nikita"})

  )
