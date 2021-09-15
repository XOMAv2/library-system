(ns utilities.api.return
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request with-relogin make-cb]])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol ReturnAPI
  (-add-user-limit [this user-limit] "")
  (-get-user-limit [this uid] "")
  (-get-user-limit-by-user-uid [this user-uid] "")
  (-get-all-user-limits [this] "")
  (-update-user-limit [this uid user-limit] "")
  (-update-total-limit-by-user-uid [this user-uid delta] "")
  (-update-available-limit-by-user-uid [this user-uid delta] "")
  (-delete-user-limit [this uid] "")
  (-get-token [this] "")
  (-refresh-token [this] "")
  (-verify-token [this] ""))

(defrecord ReturnService [uri
                          ^CircuitBreaker cb
                          ^clojure.lang.Atom token
                          client-id client-secret]
  ReturnAPI
  (-add-user-limit [this user-limit]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :post
                           :url (str (remove-trailing-slash uri) "/api/limits")
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str user-limit)})))
  (-get-user-limit [this uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/limits/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-user-limit-by-user-uid [this user-uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/limits/user-uid/" user-uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-all-user-limits [this]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/limits")
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-update-user-limit [this uid user-limit]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri) "/api/limits/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str user-limit)})))
  (-update-total-limit-by-user-uid [this user-uid delta]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri)
                                     "/api/limits/user-uid/" user-uid "/total-limit/" delta)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-update-available-limit-by-user-uid [this user-uid delta]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri)
                                     "/api/limits/user-uid/" user-uid "/available-limit/" delta)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-delete-user-limit [this uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :delete
                           :url (str (remove-trailing-slash uri) "/api/limits/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-token [this]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/login")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:client-id client-id
                                     :client-secret client-secret})}))
  (-refresh-token [this]
    (cb-sync-request cb {:method :put
                         :url (str (remove-trailing-slash uri) "​/api​/auth​/refresh")
                         :headers {"Authorization" (str "Bearer " @token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-verify-token [this]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/verify")
                         :headers {"Authorization" (str "Bearer " @token)
                                   "Accept" "application/edn; charset=utf-8"}})))

(defn make-return-service [uri cb-options client-id client-secret]
  (->ReturnService uri (make-cb cb-options) (atom nil) client-id client-secret))

(comment
  (def return-service
    (make-return-service "http://127.0.0.1:3004"
                         {:failure-threshold-ratio [3 6]
                          :delay-ms 10000}
                         "book"
                         "book"))

  (require '[diehard.circuit-breaker])

  (let [resp (-get-token return-service)
        state (diehard.circuit-breaker/state (:cb return-service))]
    [state resp])

  (:token return-service)

  (-verify-token return-service )

  (-get-all-user-limits return-service)

  (-add-user-limit return-service
                   {:user-uid (java.util.UUID/randomUUID)
                    :total-limit 43
                    :available-limit 23})

  (-get-user-limit-by-user-uid return-service
                               #uuid "b5021e12-ee69-44b1-b8fc-895f3fe86cef")

  (-update-available-limit-by-user-uid return-service
                                       #uuid "b5021e12-ee69-44b1-b8fc-895f3fe86cef" 5)

  (-update-user-limit return-service
                      #uuid "874434b5-b609-4bac-b7bc-786cce06779a" {:total-limit 120
                                                                    :available-limit 20})
  )
