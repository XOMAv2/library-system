(ns service.session.router
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
            [expound.alpha :refer [expound-str]]
            [service.session.handlers :as handlers]
            [utilities.muuntaja :refer [muuntaja-instance]]
            [utilities.schemas :as schemas :refer [message]]
            [clojure.spec.alpha :as s]
            [malli.util]
            [utilities.auth :refer [authorization-middleware backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]))

(def db-middleware
  {:name ::db-middleware
   :spec (s/keys :req-un [:service.session.system/db])
   :compile (fn [{:keys [db]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :db db)))))})

(def services-uri-middleware
  {:name ::services-uri-middleware
   :spec (s/keys :req-un [:service.session.system/services-uri])
   :compile (fn [{:keys [services-uri]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :services-uri services-uri)))))})

(defn app [db services-uri]
  (ring/ring-handler
   (ring/router
    ["/api" {:swagger {:securityDefinitions {:apiAuth {:type "apiKey"
                                                       :name "Authorization"
                                                       :in "header"}}}}
     ["/swagger.json" {:get {:no-doc true
                             :swagger {:info {:title "session-api"
                                              :description "Swagger 2.0"}}
                             :handler (create-swagger-handler)}}]
     
     ["/users" {:swagger {:tags ["users"]}}
      ["" {:get {:middleware [authorization-middleware]
                 :responses {200 {:body [:map [:users [:sequential schemas/user-out]]]}}
                 :handler handlers/get-all-users}
           :post {:services-uri services-uri
                  :middleware [services-uri-middleware]

                  :parameters {:body schemas/user-in}
                  :responses {201 {:body schemas/user-out
                                   :headers {"Location" {:schema {:type "string"}}}}
                              400 {:body [:map
                                          [:type string?]
                                          [:message string?]]}}
                  :handler handlers/add-user}}]
      ["/:uid" {:middleware [authorization-middleware]

                :parameters {:path [:map [:uid uuid?]]}

                :get {:responses {200 {:body schemas/user-out}
                                  404 {:body message}}
                      :handler handlers/get-user}
                :delete {:responses {200 {:body schemas/user-out}
                                     404 {:body message}}
                         :handler handlers/delete-user}
                :patch {:parameters {:body (malli.util/optional-keys schemas/user-in)}
                        :responses {200 {:body schemas/user-out}
                                    400 {:body [:map
                                                [:type string?]
                                                [:message string?]]}
                                    404 {:body message}}
                        :handler handlers/update-user}}]]
     
     ["/auth" {:swagger {:tags ["auth"]}}
      ["/login" {:post {:parameters {:body [:map
                                            [:email schemas/non-empty-string]
                                            [:password schemas/non-empty-string]]}
                        :responses {200 {:body schemas/token-pair}
                                    401 {:body message}
                                    404 {:body message}}
                        :handler handlers/get-tokens}}]
      ["/refresh" {:put {:parameters {:body [:map [:refresh-token schemas/non-empty-string]]}
                         :responses {200 {:body schemas/token-pair}
                                     401 {:body message}
                                     404 {:body message}}
                         :handler handlers/refresh-tokens}}]
      ["/verify" {:post {:parameters {:body [:map [:access-token schemas/non-empty-string]]}
                         :responses {200 {}
                                     401 {:body message}}
                         :handler handlers/verify-token}}]]]
    {:data {:db db
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
