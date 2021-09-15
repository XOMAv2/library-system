(ns utilities.api.book
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request with-relogin make-cb]])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol BookAPI
  (-add-book [this book] "")
  (-get-book [this uid] "")
  (-get-all-books [this] [this book] "")
  (-update-book [this uid book] "")
  (-delete-book [this uid] "")
  (-get-token [this] "")
  (-refresh-token [this] "")
  (-verify-token [this] ""))

(defrecord BookService [uri
                        ^CircuitBreaker cb
                        ^clojure.lang.Atom token
                        client-id client-secret]
  BookAPI
  (-add-book [this book]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :post
                           :url (str (remove-trailing-slash uri) "/api/books")
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str book)})))
  (-get-book [this uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/books/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-all-books [this]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/books")
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-get-all-books [this book]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :get
                           :url (str (remove-trailing-slash uri) "/api/books")
                           :query-params book
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Accept" "application/edn; charset=utf-8"}})))
  (-update-book [this uid book]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :patch
                           :url (str (remove-trailing-slash uri) "/api/books/" uid)
                           :headers {"Authorization" (str "Bearer " @token)
                                     "Content-Type" "application/edn; charset=utf-8"
                                     "Accept" "application/edn; charset=utf-8"}
                           :body (str book)})))
  (-delete-book [this uid]
    (with-relogin [#(->> this -get-token :body :token (reset! token))]
      (cb-sync-request cb {:method :delete
                           :url (str (remove-trailing-slash uri) "/api/books/" uid)
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

(defn make-book-service [uri cb-options client-id client-secret]
  (->BookService uri (make-cb cb-options) (atom nil) client-id client-secret))

(comment
  (def book-service
    (make-book-service "http://127.0.0.1:3000"
                       {:failure-threshold-ratio [3 6]
                        :delay-ms 10000}
                       "book"
                       "book"))

  (require '[diehard.circuit-breaker])

  (let [resp (-get-token book-service)
        state (diehard.circuit-breaker/state (:cb book-service))]
    [state resp])

  (:token book-service)

  (-verify-token book-service)

  (-get-all-books book-service)

  (-add-book book-service {:name "W&P3"
                           :authors ["Nikki", "Igor", "L"]
                           :genres []
                           :description "d"
                           :price 13})
  
  #_{:client-id "book"
     :client-secret "book"
     :circuit-breaker-options {:failure-threshold-ratio [3 6]
                               :delay-ms 10000}}
  
  #_(-get-all-books book-service
                  {:authors nil})
  #_(-get-all-books book-service
                  {:authors []})

  (-get-book book-service
             #uuid "b3214095-163c-4190-a49a-a2ff25b4c445")

  (-update-book book-service
                #uuid "b3214095-163c-4190-a49a-a2ff25b4c445" {:description "des"})
  )
