(ns utilities.api.session
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request with-relogin make-cb]])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol SessionAPI
  (-add-user [this user] "")
  (-get-user [this uid] "")
  (-get-all-users [this] "")
  (-update-user [this uid user] "")
  (-delete-user [this uid] "")
  (-get-tokens [this] "")
  (-refresh-tokens [this] "")
  (-verify-token [this] ""))

#_(let [{a-token :access-token r-token :refresh-token} (-> this -get-tokens :body)]
  (reset! access-token a-token)
  (reset! refresh-token r-token))
#_"TODO: Fix refresh and verify funcitons."

(defrecord SessionService [uri
                           ^CircuitBreaker cb
                           ^clojure.lang.Atom access-token
                           ^clojure.lang.Atom refresh-token
                           email password]
  SessionAPI
  (-add-user [this user]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/users")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str user)}))
  (-get-user [this uid]
    (with-relogin [#(->> this -get-tokens :body :access-token (reset! access-token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/users/" uid)
                           :headers {"Authorization" (str "Bearer " @access-token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-all-users [this]
    (with-relogin [#(->> this -get-tokens :body :access-token (reset! access-token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/users")
                           :headers {"Authorization" (str "Bearer " @access-token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-update-user [this uid user]
    (with-relogin [#(->> this -get-tokens :body :access-token (reset! access-token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri) "/api/users/" uid)
                           :headers {"Authorization" (str "Bearer " @access-token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str user)})))
  (-delete-user [this uid]
    (with-relogin [#(->> this -get-tokens :body :access-token (reset! access-token))]
      (cb-sync-request cb {:method :delete
                           :url (str (remove-trailing-slash uri) "/api/users/" uid)
                           :headers {"Authorization" (str "Bearer " @access-token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-tokens [this]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/login")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:email email
                                     :password password})}))
  (-refresh-tokens [this]
    (cb-sync-request cb {:method :put
                         :url (str (remove-trailing-slash uri) "​/api​/auth​/refresh")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body {:refresh-token @refresh-token}}))
  (-verify-token [this]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/verify")
                         :headers {"Content-type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body {:access-token @access-token}})))

(defn make-session-service [uri cb-options email password]
  (->SessionService uri (make-cb cb-options) (atom nil) (atom nil) email password))

(comment
  (def session-service
    (make-session-service "http://127.0.0.1:3005"
                          {:failure-threshold-ratio [3 6]
                           :delay-ms 10000}
                          "admin@admin.com"
                          "admin"))

  (require '[diehard.circuit-breaker])

  (let [resp (-get-tokens session-service)
        state (diehard.circuit-breaker/state (:cb session-service))]
    [state resp])
  
  @(:access-token session-service)
  @(:refresh-token session-service)

  (-verify-token session-service)

  (-refresh-tokens session-service)

  (-get-all-users session-service)

  (-add-user session-service {:name "Nikki"
                              :email "Nikki"
                              :role "admin"
                              :password "Nikki"})

  (-get-user session-service
             #uuid "51a063a3-488d-4ece-86d8-83d679c28d52")

  (-update-user session-service
                #uuid "51a063a3-488d-4ece-86d8-83d679c28d52" {:name "Nikita"})

  )
