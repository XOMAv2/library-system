(ns service.library.router
  (:require [reitit.ring :as ring]
            [reitit.ring.spec]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli]
            [reitit.swagger :refer [create-swagger-handler] :as swagger]
            [reitit.swagger-ui :refer [create-swagger-ui-handler]]
            [reitit.ring.middleware.dev :refer [print-request-diffs]]
            [reitit.ring.middleware.exception :refer [exception-middleware]]
            [reitit.ring.middleware.muuntaja :refer [format-negotiate-middleware
                                                     format-request-middleware
                                                     format-response-middleware]]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [utilities.middlewares :refer [request->stats-middleware
                                           response->stats-middleware]]
            [expound.alpha :refer [expound-str]]
            [service.library.handlers.library :as l-handlers]
            [service.library.handlers.library-book :as lb-handlers]
            [service.library.handlers.order :as o-handlers]
            [utilities.handlers.auth :as a-handlers]
            [utilities.muuntaja :refer [muuntaja-instance]]
            [utilities.schemas :as schemas :refer [message]]
            [clojure.spec.alpha :as s]
            [utilities.auth :refer [authorization-middleware backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]))

"
•	получение списка информации о всех библиотеках в системе;
•	получение информации о библиотеке;
•	удаление информации о библиотеке;
•	добавление информации о библиотеке;
•	добавление книги в фонд библиотеки;
•	удаление книги из фонда библиотеки;
•	изменение числа экземпляров книги в фонде библиотеке;
•	бронирование книги;
•	возврат книги;

•	получение истории заказов пользователя;
•	добавление заказа в историю;
•	удаление заказа из истории;
•	создание пустой истории для нового пользователя;
•	удаление существующего пользователя и его истории;
•	изменение лимита книг на руках пользователя;
•	получение книжного лимита пользователя.
"

(def db-middleware
  {:name ::db-middleware
   :spec (s/keys :req-un [:service.library.system/db])
   :compile (fn [{:keys [db]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :db db)))))})

(def services-uri-middleware
  {:name ::services-uri-middleware
   :spec (s/keys :req-un [:service.library.system/services-uri])
   :compile (fn [{:keys [services-uri]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :services-uri services-uri)))))})

#_"TODO: swagger query params explode"

(defn app [db services services-uri]
  (ring/ring-handler
   (ring/router
    ["/api" {:swagger {:securityDefinitions {:apiAuth {:type "apiKey"
                                                       :name "Authorization"
                                                       :in "header"}}}}
     ["/swagger.json" {:get {:no-doc true
                             :swagger {:info {:title "library-api"
                                              :description "Swagger 2.0"}}
                             :handler (create-swagger-handler)}}]

     ["/libraries" {:swagger {:tags ["libraries"]}

                    :roles #{"admin"}
                    :middleware [authorization-middleware]}
      ["" {:get {:parameters {:query schemas/library-query}
                 :responses {200 {:body [:map [:libraries [:sequential schemas/library-out]]]}}
                 :handler l-handlers/get-all-libraries}
           :post {:services-uri services-uri
                  :middleware [services-uri-middleware]

                  :parameters {:body schemas/library-add}
                  :responses {201 {:body schemas/library-out
                                   :headers {"Location" {:schema {:type "string"}}}}
                              400 {:body [:map
                                          [:type string?]
                                          [:message string?]]}}
                  :handler l-handlers/add-library}}]
      ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

                :get {:responses {200 {:body schemas/library-out}
                                  404 {:body message}}
                      :handler l-handlers/get-library}
                :delete {:responses {200 {:body schemas/library-out}
                                     404 {:body message}}
                         :handler l-handlers/delete-library}
                :patch {:parameters {:body schemas/library-update}
                        :responses {200 {:body schemas/library-out}
                                    400 {:body [:map
                                                [:type string?]
                                                [:message string?]]}
                                    404 {:body message}}
                        :handler l-handlers/update-library}}]]
     
     ["/library-books" {:swagger {:tags ["library-books"]}

                 :roles #{"admin"}
                 :middleware [authorization-middleware]}
      ["" {:get {:parameters {:query schemas/library-book-query}
                 :responses {200 {:body [:map [:library-books [:sequential schemas/library-book-out]]]}}
                 :handler lb-handlers/get-all-library-books}
           :post {:services-uri services-uri
                  :middleware [services-uri-middleware]

                  :parameters {:body schemas/library-book-add}
                  :responses {201 {:body schemas/library-book-out
                                   :headers {"Location" {:schema {:type "string"}}}}
                              400 {:body [:map
                                          [:type string?]
                                          [:message string?]]}}
                  :handler lb-handlers/add-library-book}}]
      ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

                :get {:responses {200 {:body schemas/library-book-out}
                                  404 {:body message}}
                      :handler lb-handlers/get-library-book}
                :delete {:responses {200 {:body schemas/library-book-out}
                                     404 {:body message}}
                         :handler lb-handlers/delete-library-book}
                :put {:responses {200 {:body schemas/library-book-out}
                                   404 {:body message}}
                       :handler lb-handlers/restore-library-book}
                :patch {:parameters {:body schemas/library-book-update}
                        :responses {200 {:body schemas/library-book-out}
                                    400 {:body [:map
                                                [:type string?]
                                                [:message string?]]}
                                    404 {:body message}}
                        :handler lb-handlers/update-library-book}}]]

     ["/orders" {:swagger {:tags ["orders"]}

                 :roles #{"admin"}
                 :middleware [authorization-middleware]}
      ["" {:get {:parameters {:query schemas/order-query}
                 :responses {200 {:body [:map [:orders [:sequential schemas/order-out]]]}}
                 :handler o-handlers/get-all-orders}
           :post {:services-uri services-uri
                  :middleware [services-uri-middleware]

                  :parameters {:body schemas/order-add}
                  :responses {201 {:body schemas/order-out
                                   :headers {"Location" {:schema {:type "string"}}}}
                              400 {:body [:map
                                          [:type string?]
                                          [:message string?]]}}
                  :handler o-handlers/add-order}}]
      ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

                :get {:responses {200 {:body schemas/order-out}
                                  404 {:body message}}
                      :handler o-handlers/get-order}
                :delete {:responses {200 {:body schemas/order-out}
                                     404 {:body message}}
                         :handler o-handlers/delete-order}
                :patch {:parameters {:body schemas/order-update}
                        :responses {200 {:body schemas/order-out}
                                    400 {:body [:map
                                                [:type string?]
                                                [:message string?]]}
                                    404 {:body message}}
                        :handler o-handlers/update-order}}]]

     ["/auth" {:swagger {:tags ["auth"]}}
      ["/login" {:post {:parameters {:body [:map
                                            [:client-id schemas/non-empty-string]
                                            [:client-secret schemas/non-empty-string]]}
                        :responses {200 {:body [:map [:token schemas/non-empty-string]]}
                                    401 {:body message}
                                    404 {:body message}}
                        :handler a-handlers/get-token}}]
      ["/refresh" {:put {:roles nil
                         :middleware [authorization-middleware]

                         :responses {200 {:body [:map [:token schemas/non-empty-string]]}
                                     404 {:body message}}
                         :handler a-handlers/refresh-token}}]
      ["/verify" {:post {:roles nil
                         :middleware [authorization-middleware]

                         :responses {200 {}}
                         :handler a-handlers/verify-token}}]]]
    {:data {:db db
            :services services
            :stats/service "library"
            :coercion reitit.coercion.malli/coercion #_"Schemas closing, extra keys stripping, ..."
            #_"... transformers adding for json-body, path and query params."
            :muuntaja muuntaja-instance
            :middleware [swagger/swagger-feature #_"Swagger feature."
                         format-negotiate-middleware #_"Content negotiation."
                         format-response-middleware #_"Response body encoding."
                         exception-middleware #_"Exception handling."
                         db-middleware #_"Assoc :db key to request map."
                         parameters-middleware #_"Query-params and form-params extraction."
                         format-request-middleware #_"Request body decoding."
                         coercion/coerce-response-middleware #_"Response bodys coercion."
                         coercion/coerce-request-middleware #_"Request parameters coercion."
                         request->stats-middleware
                         response->stats-middleware
                         [wrap-authentication backend] #_"Obtaining data from authorization header."]}
     #_#_:reitit.middleware/transform print-request-diffs #_"Middleware chain transformation."
     :validate reitit.ring.spec/validate #_"Routes structure validation."
     :reitit.spec/explain expound-str #_"Routes structure error explanation."})
   (ring/routes
    (create-swagger-ui-handler {:path "/api/swagger"
                                :url "/api/swagger.json"
                                :config {:validatorUrl nil
                                         :operationsSorter "alpha"}})
    (ring/redirect-trailing-slash-handler)
    (ring/create-default-handler
     {:not-found
      (constantly {:status 404
                   :body "{\"message\": \"No route matched.\"}"
                   :headers {"Content-Type" "application/json; charset=utf-8"}})
      :method-not-allowed
      (constantly {:status 405
                   :body "{\"message\": \"No method matched for the specified uri.\"}"
                   :headers {"Content-Type" "application/json; charset=utf-8"}})
      #_#_:not-acceptable "(406) (handler returned nil)."}))))
