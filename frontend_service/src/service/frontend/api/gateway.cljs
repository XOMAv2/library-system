(ns service.frontend.api.gateway
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [ajax.edn]
            [service.frontend.api.core
             :refer [def-request-event make-request]
             :rename {def-request-event def-re
                      make-request mr}]
            [utilities.core :refer [remove-trailing-slash]]))

#_"The API consumer must override the following two coeffects."
#_(rf/reg-cofx :service.frontend.api.gateway/uri
  (fn [coeffects _]
    (assoc coeffects :uri "http://localhost:3001")))

#_(rf/reg-cofx :service.frontend.api.gateway/tokens-path
  (fn [coeffects _]
    (assoc coeffects :tokens-path :tokens)))

#_"users"
(rf/reg-event-fx ::add-user
  [(rf/inject-cofx ::uri)]
  (fn [{:keys [uri]} [_ on-success on-failure user]]
    {:http-xhrio {:method :post
                  :uri (str (remove-trailing-slash uri) "/api/users")
                  :body (str user)
                  :headers {"Content-Type" "application/edn; charset=utf-8"}
                  :response-format (ajax.edn/edn-response-format)
                  :on-success on-success
                  :on-failure on-failure}}))

(def-re ::get-user [uid]         (mr :get (str "/api/users/" uid)))
(def-re ::get-all-users []       (mr :get "/api/users"))
(def-re ::update-user [uid user] (mr :patch (str "/api/users/" uid) user))
(def-re ::delete-user [uid]      (mr :delete (str "/api/users/" uid)))
(def-re ::restore-user [uid]     (mr :put (str "/api/users/" uid)))

#_"auth"
(rf/reg-event-fx ::get-tokens
  [(rf/inject-cofx ::uri)]
  (fn [{:keys [uri]} [_ on-success on-failure email password]]
    {:http-xhrio {:method :post
                  :uri (str (remove-trailing-slash uri) "/api/auth/login")
                  :body (str {:email email
                              :password password})
                  :headers {"Content-Type" "application/edn; charset=utf-8"}
                  :response-format (ajax.edn/edn-response-format)
                  :on-success on-success
                  :on-failure on-failure}}))

(rf/reg-event-fx ::refresh-tokens
  [(rf/inject-cofx ::uri)]
  (fn [{:keys [uri]} [_ on-success on-failure refresh-token]]
    {:http-xhrio {:method :put
                  :uri (str (remove-trailing-slash uri) "/api/auth/refresh")
                  :body (str {:refresh-token refresh-token})
                  :headers {"Content-Type" "application/edn; charset=utf-8"}
                  :response-format (ajax.edn/edn-response-format)
                  :on-success on-success
                  :on-failure on-failure}}))

(rf/reg-event-fx ::verify-token
  [(rf/inject-cofx ::uri)]
  (fn [{:keys [uri]} [_ on-success on-failure access-token]]
    {:http-xhrio {:method :post
                  :uri (str (remove-trailing-slash uri) "/api/auth/verify")
                  :body (str {:access-token access-token})
                  :headers {"Content-Type" "application/edn; charset=utf-8"}
                  :response-format (ajax.edn/edn-response-format)
                  :on-success on-success
                  :on-failure on-failure}}))

#_"books"
(def-re ::add-book [book]            (mr :post "/api/books" book))
(def-re ::get-book [uid]             (mr :get (str "/api/books/" uid)))
(def-re ::get-all-books [book-query] (mr :get "/api/books" nil book-query))
(def-re ::update-book [uid book]     (mr :patch (str "/api/books/" uid) book))
(def-re ::delete-book [uid]          (mr :delete (str "/api/books/" uid)))
(def-re ::restore-book [uid]         (mr :put (str "/api/books/" uid)))

#_"libraries"
(def-re ::add-library [library]             (mr :post "/api/libraries" library))
(def-re ::get-library [uid]                 (mr :get (str "/api/libraries/" uid)))
(def-re ::get-all-libraries [library-query] (mr :get "/api/libraries" nil library-query))
(def-re ::update-library [uid library]      (mr :patch (str "/api/libraries/" uid) library))
(def-re ::delete-library [uid]              (mr :delete (str "/api/libraries/" uid)))

#_"library-books"
(def-re ::add-library-book [library-book]                (mr :post "/api/library-books" library-book))
(def-re ::get-library-book [uid]                         (mr :get (str "/api/library-books/" uid)))
(def-re ::get-all-library-books [library-book-query]     (mr :get "/api/library-books" nil library-book-query))
(def-re ::update-library-book [uid library-book]         (mr :patch (str "/api/library-books/" uid) library-book))
(def-re ::delete-library-book [uid]                      (mr :delete (str "/api/library-books/" uid)))
(def-re ::delete-all-library-books [library-book-query]  (mr :delete "/api/library-books" nil library-book-query))
(def-re ::restore-library-book [uid]                     (mr :put (str "/api/library-books/" uid)))
(def-re ::restore-all-library-books [library-book-query] (mr :put "/api/library-books" nil library-book-query))

#_"orders"
(def-re ::add-order [order]                      (mr :post "/api/orders" order))
(def-re ::get-order [uid]                        (mr :get (str "/api/orders/" uid)))
(def-re ::get-all-orders [order-query]           (mr :get "/api/orders" nil order-query))
(def-re ::update-order [uid order]               (mr :patch (str "/api/orders/" uid) order))
(def-re ::update-all-orders [order-query order]  (mr :patch "/api/orders" order order-query))
(def-re ::delete-order [uid]                     (mr :delete (str "/api/orders/" uid)))

#_"ratings"
(def-re ::add-user-rating [user-rating]              (mr :post "/api/ratings" user-rating))
(def-re ::get-user-rating [uid]                      (mr :get (str "/api/ratings/" uid)))
(def-re ::get-user-rating-by-user-uid [user-uid]     (mr :get (str "/api/ratings/user-uid/" user-uid)))
(def-re ::get-all-user-ratings []                    (mr :get "/api/ratings"))
(def-re ::update-user-rating [uid user-rating]       (mr :patch (str "/api/ratings/" uid) user-rating))
(def-re ::update-rating-by-user-uid [user-uid condition] (mr :patch (str "/api/ratings/user-uid/" user-uid "/rating/" condition)))
(def-re ::delete-user-rating [uid]                   (mr :delete (str "/api/ratings/" uid)))
(def-re ::delete-user-rating-by-user-uid [user-uid]  (mr :delete (str "/api/ratings/user-uid/" user-uid)))
(def-re ::restore-user-rating [uid]                  (mr :put (str "/api/ratings/" uid)))
(def-re ::restore-user-rating-by-user-uid [user-uid] (mr :put (str "/api/ratings/user-uid/" user-uid)))

#_"limits"
(def-re ::add-user-limit [user-limit]               (mr :post "/api/limits" user-limit))
(def-re ::get-user-limit [uid]                      (mr :get (str "/api/limits/" uid)))
(def-re ::get-user-limit-by-user-uid [user-uid]     (mr :get (str "/api/limits/user-uid/" user-uid)))
(def-re ::get-all-user-limits []                    (mr :get "/api/limits"))
(def-re ::update-user-limit [uid user-limit]        (mr :patch (str "/api/limits/" uid) user-limit))
(def-re ::reset-total-limit-by-user-uid [user-uid value] (mr :post (str "/api/limits/user-uid/" user-uid "/total-limit/" value)))
(def-re ::update-total-limit-by-user-uid [user-uid delta] (mr :patch (str "/api/limits/user-uid/" user-uid "/total-limit/" delta)))
(def-re ::update-available-limit-by-user-uid [user-uid delta] (mr :patch (str "/api/limits/user-uid/" user-uid "/available-limit/" delta)))
(def-re ::delete-user-limit [uid]                   (mr :delete (str "/api/limits/" uid)))
(def-re ::delete-user-limit-by-user-uid [user-uid]  (mr :delete (str "/api/limits/user-uid/" user-uid)))
(def-re ::restore-user-limit [uid]                  (mr :put (str "/api/limits/" uid)))
(def-re ::restore-user-limit-by-user-uid [user-uid] (mr :put (str "/api/limits/user-uid/" user-uid)))

#_"stats"
(def-re ::add-stat-record [stat-record]        (mr :post "/api/stats" stat-record))
(def-re ::get-stat-record [uid]                (mr :get (str "/api/stats/" uid)))
(def-re ::get-all-stat-records [service]       (when service
                                                 (mr :get "/api/stats" nil {:service service})
                                                 (mr :get "/api/stats")))
(def-re ::update-stat-record [uid stat-record] (mr :patch (str "/api/stats/" uid) stat-record))
(def-re ::delete-stat-record [uid]             (mr :delete (str "/api/stats/" uid)))
(def-re ::delete-all-stat-records []           (mr :delete "/api/stats"))