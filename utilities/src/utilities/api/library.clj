(ns utilities.api.library
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request with-relogin make-cb make-request]])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol LibraryAPI
  (-add-library [this library] "")
  (-get-library [this uid] "")
  (-get-all-libraries [this] "")
  (-update-library [this uid library] "")
  (-delete-library [this uid] "")

  (-add-library-book [this library-book] "")
  (-get-library-book [this uid] "")
  (-get-all-library-books [this] "")
  (-update-library-book [this uid library-book] "")
  (-delete-library-book [this uid] "")
  (-restore-library-book [this uid] "")

  (-add-order [this order] "")
  (-get-order [this uid] "")
  (-get-all-orders [this] "")
  (-update-order [this uid order] "")
  (-delete-order [this uid] "")

  (-get-token [this] "")
  (-refresh-token [this] "")
  (-verify-token [this] ""))

(defrecord LibraryService [uri
                           ^CircuitBreaker cb
                           ^clojure.lang.Atom token
                           client-id client-secret]
  LibraryAPI
  #_"libraries"
  (-add-library [this library]        (make-request :post "/api/libraries" library))
  (-get-library [this uid]            (make-request :get (str "/api/libraries/" uid)))
  (-get-all-libraries [this]          (make-request :get "/api/libraries"))
  (-update-library [this uid library] (make-request :patch (str "/api/libraries/" uid) library))
  (-delete-library [this uid]         (make-request :delete (str "/api/libraries/" uid)))
  #_"library-books"
  (-add-library-book [this library-book] (make-request :post "/api/library-books" library-book))
  (-get-library-book [this uid]          (make-request :get (str "/api/library-books/" uid)))
  (-get-all-library-books [this]         (make-request :get "/api/library-books"))
  (-update-library-book [this uid library-book] (make-request :patch (str "/api/library-books/" uid) library-book))
  (-delete-library-book [this uid]       (make-request :delete (str "/api/library-books/" uid)))
  (-restore-library-book [this uid]      (make-request :put (str "/api/library-books/" uid)))
  #_"orders"
  (-add-order [this order]        (make-request :post "/api/orders" order))
  (-get-order [this uid]          (make-request :get (str "/api/orders/" uid)))
  (-get-all-orders [this]         (make-request :get "/api/orders"))
  (-update-order [this uid order] (make-request :patch (str "/api/orders/" uid) order))
  (-delete-order [this uid]       (make-request :delete (str "/api/orders/" uid)))
  #_"auth"
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

(defn make-library-service [uri cb-options client-id client-secret]
  (->LibraryService uri (make-cb cb-options) (atom nil) client-id client-secret))

(comment
  (def library-service
    (make-library-service "http://127.0.0.1:3002"
                         {:failure-threshold-ratio [3 6]
                          :delay-ms 10000}
                         "book"
                         "book"))

  (require '[diehard.circuit-breaker])

  (let [resp (-get-token library-service)
        state (diehard.circuit-breaker/state (:cb library-service))]
    [state resp])

  (:token library-service)

  (-verify-token library-service)

  (-get-all-libraries library-service)
  
  )
