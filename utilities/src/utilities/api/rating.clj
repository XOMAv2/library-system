(ns utilities.api.rating
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request with-relogin make-cb]])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol RatingAPI
  (-add-user-rating [this user-rating] "")
  (-get-user-rating [this uid] "")
  (-get-user-rating-by-user-uid [this user-uid] "")
  (-get-all-user-ratings [this] "")
  (-update-user-rating [this uid user-rating] "")
  (-update-rating-by-user-uid [this user-uid delta] "")
  (-delete-user-rating [this uid] "")
  (-get-token [this] "")
  (-refresh-token [this] "")
  (-verify-token [this] ""))

(defrecord RatingService [uri
                          ^CircuitBreaker cb
                          ^clojure.lang.Atom token
                          client-id client-secret]
  RatingAPI
  (-add-user-rating [this user-rating]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :post
                           :url (str (remove-trailing-slash uri) "/api/ratings")
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str user-rating)})))
  (-get-user-rating [this uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/ratings/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-user-rating-by-user-uid [this user-uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/ratings/user-uid/" user-uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-all-user-ratings [this]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/ratings")
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-update-user-rating [this uid user-rating]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri) "/api/ratings/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str user-rating)})))
  (-update-rating-by-user-uid [this user-uid delta]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri)
                                     "/api/ratings/user-uid/" user-uid "/rating/" delta)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-delete-user-rating [this uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :delete
                           :url (str (remove-trailing-slash uri) "/api/ratings/" uid)
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

(defn make-rating-service [uri cb-options client-id client-secret]
  (->RatingService uri (make-cb cb-options) (atom nil) client-id client-secret))

(comment
  (def rating-service
    (make-rating-service "http://127.0.0.1:3003"
                         {:failure-threshold-ratio [3 6]
                          :delay-ms 10000}
                         "book"
                         "book"))
  
  (require '[diehard.circuit-breaker])

  (let [resp (-get-token rating-service)
        state (diehard.circuit-breaker/state (:cb rating-service))]
    [state resp])
  
  (:token rating-service)

  (-verify-token rating-service)

  (-get-all-user-ratings rating-service)

  (-add-user-rating rating-service
                    {:user-uid (java.util.UUID/randomUUID)
                     :rating 43})

  (-get-user-rating-by-user-uid rating-service
                                #uuid "80e0c31e-ca3b-4edb-9065-423a9370588d")

  (-update-rating-by-user-uid rating-service
                              #uuid "80e0c31e-ca3b-4edb-9065-423a9370588d" 10)

  (-update-user-rating rating-service
                       #uuid "011d5c70-fe90-4cde-a04a-43406f741bf8" {:rating 10})
  )
