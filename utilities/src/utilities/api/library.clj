(ns utilities.api.library
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core
             :refer [cb-sync-request with-relogin make-cb make-request]
             :rename {make-request mk}])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol LibraryAPI
  (-add-library [this library] "")
  (-get-library [this uid] "")
  (-get-all-libraries [this] [this library-query] "")
  (-update-library [this uid library] "")
  (-delete-library [this uid] "")

  (-add-library-book [this library-book] "")
  (-get-library-book [this uid] "")
  (-get-all-library-books [this] [this library-book-query] "")
  (-update-library-book [this uid library-book] "")
  (-delete-library-book [this uid] "")
  (-delete-all-library-books [this library-book-query] "")
  (-restore-library-book [this uid] "")
  (-restore-all-library-books [this library-book-query] "")

  (-add-order [this order] "")
  (-get-order [this uid] "")
  (-get-all-orders [this] [this order-query] "")
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
  (-add-library [this library]             (mk :post "/api/libraries" library))
  (-get-library [this uid]                 (mk :get (str "/api/libraries/" uid)))
  (-get-all-libraries [this]               (mk :get "/api/libraries"))
  (-get-all-libraries [this library-query] (mk :get "/api/libraries" nil library-query))
  (-update-library [this uid library]      (mk :patch (str "/api/libraries/" uid) library))
  (-delete-library [this uid]              (mk :delete (str "/api/libraries/" uid)))
  #_"library-books"
  (-add-library-book [this library-book]                (mk :post "/api/library-books" library-book))
  (-get-library-book [this uid]                         (mk :get (str "/api/library-books/" uid)))
  (-get-all-library-books [this]                        (mk :get "/api/library-books"))
  (-get-all-library-books [this library-book-query]     (mk :get "/api/library-books" nil library-book-query))
  (-update-library-book [this uid library-book]         (mk :patch (str "/api/library-books/" uid) library-book))
  (-delete-library-book [this uid]                      (mk :delete (str "/api/library-books/" uid)))
  (-delete-all-library-books [this library-book-query]  (mk :delete "/api/library-books" nil library-book-query))
  (-restore-library-book [this uid]                     (mk :put (str "/api/library-books/" uid)))
  (-restore-all-library-books [this library-book-query] (mk :put "/api/library-books" nil library-book-query))
  #_"orders"
  (-add-order [this order]                      (mk :post "/api/orders" order))
  (-get-order [this uid]                        (mk :get (str "/api/orders/" uid)))
  (-get-all-orders [this]                       (mk :get "/api/orders"))
  (-get-all-orders [this order-query]           (mk :get "/api/orders" nil order-query))
  (-update-order [this uid order]               (mk :patch (str "/api/orders/" uid) order))
  (-delete-order [this uid]                     (mk :delete (str "/api/orders/" uid)))

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
