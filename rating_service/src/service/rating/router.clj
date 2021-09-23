(ns service.rating.router
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
            [service.rating.handlers :as handlers]
            [utilities.handlers.auth :as a-handlers]
            [utilities.muuntaja :refer [muuntaja-instance]]
            [utilities.schemas :as schemas :refer [message]]
            [clojure.spec.alpha :as s]
            [utilities.auth :refer [authorization-middleware backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]))

(def db-middleware
  {:name ::db-middleware
   :spec (s/keys :req-un [:service.rating.system/db])
   :compile (fn [{:keys [db]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :db db)))))})

(def services-uri-middleware
  {:name ::services-uri-middleware
   :spec (s/keys :req-un [:service.rating.system/services-uri])
   :compile (fn [{:keys [services-uri]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :services-uri services-uri)))))})

(defn app [db services services-uri]
  (ring/ring-handler
   (ring/router
    ["/api" {:swagger {:securityDefinitions {:apiAuth {:type "apiKey"
                                                       :name "Authorization"
                                                       :in "header"}}}}
     ["/swagger.json" {:get {:no-doc true
                             :swagger {:info {:title "rating-api"
                                              :description "Swagger 2.0"}}
                             :handler (create-swagger-handler)}}]

     ["/ratings" {:swagger {:tags ["ratings"]}

                  :roles #{"admin"}
                  :middleware [authorization-middleware]}
      ["" {:get {:responses {200 {:body [:map [:ratings [:sequential schemas/user-rating-out]]]}}
                 :handler handlers/get-all-user-ratings}
           :post {:services-uri services-uri
                  :middleware [services-uri-middleware]

                  :parameters {:body schemas/user-rating-add}
                  :responses {201 {:body schemas/user-rating-out
                                   :headers {"Location" {:schema {:type "string"}}}}
                              422 {:body [:map
                                          [:type string?]
                                          [:message string?]]}}
                  :handler handlers/add-user-rating}}]
      ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

                :get {:responses {200 {:body schemas/user-rating-out}
                                  404 {:body message}}
                      :handler handlers/get-user-rating}
                :delete {:responses {200 {:body schemas/user-rating-out}
                                     404 {:body message}}
                         :handler handlers/delete-user-rating}
                :put {:responses {200 {:body schemas/user-rating-out}
                                  404 {:body message}}
                      :handler handlers/restore-user-rating}
                :patch {:parameters {:body schemas/user-rating-update}
                        :responses {200 {:body schemas/user-rating-out}
                                    422 {:body [:map
                                                [:type string?]
                                                [:message string?]]}
                                    404 {:body message}}
                        :handler handlers/update-user-rating}}]
      ["/user-uid/:user-uid" {:get {:parameters {:path [:map [:user-uid uuid?]]}
                                    :responses {200 {:body schemas/user-rating-out}
                                                404 {:body message}}
                                    :handler handlers/get-user-rating-by-user-uid}
                              :delete {:responses {200 {:body schemas/user-rating-out}
                                                   404 {:body message}}
                                       :handler handlers/delete-user-rating-by-user-uid}
                              :put {:responses {200 {:body schemas/user-rating-out}
                                                404 {:body message}}
                                    :handler handlers/restore-user-rating-by-user-uid}}]
      ["/user-uid/:user-uid/rating/:delta"
       {:patch {:parameters {:path [:map
                                    [:user-uid uuid?]
                                    [:delta int?]]}
                :responses {200 {:body schemas/user-rating-out}
                            422 {:body [:map
                                        [:type string?]
                                        [:message string?]]}
                            404 {:body message}}
                :handler handlers/update-rating-by-user-uid}}]]

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
            :stats/service "rating"
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
