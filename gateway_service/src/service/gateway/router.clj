(ns service.gateway.router
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
            [service.gateway.routes.book :refer [book-routes]]
            [service.gateway.routes.library :refer [library-routes]]
            [service.gateway.routes.rating :refer [rating-routes]]
            [service.gateway.routes.return :refer [return-routes]]
            [service.gateway.routes.session :refer [session-routes]]
            [service.gateway.routes.stats :refer [stats-routes]]
            [utilities.muuntaja :refer [muuntaja-instance]]
            [clojure.spec.alpha :as s]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def services-middleware
  {:name ::services-middleware
   :spec (s/keys :req-un [:service.gateway.system/services])
   :compile (fn [{:keys [services]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :services services)))))})

#_"TODO: swagger query params explode"

(defn app [{:keys [services client-id]}]
  (ring/ring-handler
   (ring/router
    ["/api" {:swagger {:securityDefinitions {:apiAuth {:type "apiKey"
                                                       :name "Authorization"
                                                       :in "header"}}}}
     ["/swagger.json" {:get {:no-doc true
                             :swagger {:info {:title "gateway-api"
                                              :description "Swagger 2.0"}}
                             :handler (create-swagger-handler)}}]
     ["" {:responses {500 {:body any?}}}
      book-routes
      library-routes
      rating-routes
      return-routes
      session-routes
      stats-routes]]
    {:data {:services services
            :stats/service client-id
            :coercion reitit.coercion.malli/coercion #_"Schemas closing, extra keys stripping, ..."
            #_"... transformers adding for json-body, path and query params."
            :muuntaja muuntaja-instance
            :middleware [swagger/swagger-feature #_"Swagger feature."
                         format-negotiate-middleware #_"Content negotiation."
                         format-response-middleware #_"Response body encoding."
                         exception-middleware #_"Exception handling."
                         parameters-middleware #_"Query-params and form-params extraction."
                         format-request-middleware #_"Request body decoding."
                         coercion/coerce-response-middleware #_"Response bodys coercion."
                         coercion/coerce-request-middleware #_"Request parameters coercion."
                         request->stats-middleware
                         response->stats-middleware
                         authentication-middleware #_"Obtaining data from authorization header."
                         services-middleware]}
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
