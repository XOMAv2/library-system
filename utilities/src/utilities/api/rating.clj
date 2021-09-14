(ns utilities.api.rating
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request]]))

(defprotocol RatingAPI
  (-add-user-rating [this token user-rating] "")
  (-get-user-rating [this token uid] "")
  (-get-user-rating-by-user-uid [this token user-uid] "")
  (-get-all-user-ratings [this token] "")
  (-update-user-rating [this token uid user-rating] "")
  (-update-rating-by-user-uid [this token user-uid delta] "")
  (-delete-user-rating [this token uid] "")
  (-get-token [this client-id client-secret] "")
  (-refresh-token [this token] "")
  (-verify-token [this token] ""))

(defrecord RatingService [uri cb]
  RatingAPI
  (-add-user-rating [this token user-rating]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/ratings")
                         :headers {"Authorization" (str "Bearer " token)
                                   "Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str user-rating)}))
  (-get-user-rating [this token uid]
    (cb-sync-request cb {:method :get
                         :url (str (remove-trailing-slash uri) "/api/ratings/" uid)
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-get-user-rating-by-user-uid [this token user-uid]
    (cb-sync-request cb {:method :get
                         :url (str (remove-trailing-slash uri) "/api/ratings/user-uid/" user-uid)
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-get-all-user-ratings [this token]
    (cb-sync-request cb {:method :get
                         :url (str (remove-trailing-slash uri) "/api/ratings")
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-update-user-rating [this token uid user-rating]
    (cb-sync-request cb {:method :patch
                         :url (str (remove-trailing-slash uri) "/api/ratings/" uid)
                         :headers {"Authorization" (str "Bearer " token)
                                   "Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str user-rating)}))
  (-update-rating-by-user-uid [this token user-uid delta]
    (cb-sync-request cb {:method :patch
                         :url (str (remove-trailing-slash uri)
                                   "/api/ratings/user-uid/" user-uid "/rating/" delta)
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-delete-user-rating [this token uid]
    (cb-sync-request cb {:method :delete
                         :url (str (remove-trailing-slash uri) "/api/ratings/" uid)
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-get-token [this client-id client-secret]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/login")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:client-id client-id
                                     :client-secret client-secret})}))
  (-refresh-token [this token]
    (cb-sync-request cb {:method :put
                         :url (str (remove-trailing-slash uri) "​/api​/auth​/refresh")
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-verify-token [this token]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/verify")
                         :headers {"Authorization" (str "Bearer " token)
                                   "Accept" "application/edn; charset=utf-8"}})))

(comment
  (require '[diehard.circuit-breaker])

  (def my-cb
    (utilities.api.core/make-cb {:failure-threshold-ratio [3 6]
                                 :delay-ms 10000}))

  (def rating-service
    (->RatingService "http://127.0.0.1:3003" my-cb))

  (let [resp (-get-token rating-service "book" "book")
        state (diehard.circuit-breaker/state my-cb)]
    [state resp])

  (def tk
    "eyJhbGciOiJIUzI1NiJ9.eyJ1aWQiOiI1MTI0ZWU1Mi1iMGRhLTQ1YTctOGM4NC04MDYzNjU0YzFmNTYiLCJyb2xlIjoiYWRtaW4iLCJleHAiOjE2MzQxNTExMjd9.rOsVj0S4ybYAuHqAOm_c7gEihUZrI1nDYcAXdd12GyQ")

  (-verify-token rating-service tk)

  (-get-all-user-ratings rating-service tk)

  (-add-user-rating rating-service tk
                    {:user-uid (java.util.UUID/randomUUID)
                     :rating 43})

  (-get-user-rating-by-user-uid rating-service tk
                                #uuid "80e0c31e-ca3b-4edb-9065-423a9370588d")

  (-update-rating-by-user-uid rating-service tk
                              #uuid "80e0c31e-ca3b-4edb-9065-423a9370588d" 10)

  (-update-user-rating rating-service tk
                       #uuid "011d5c70-fe90-4cde-a04a-43406f741bf8" {:rating 10})
  )
