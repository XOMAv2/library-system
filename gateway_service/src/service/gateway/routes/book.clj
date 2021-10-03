(ns service.gateway.routes.book
  (:require [utilities.api.book :as book-api]
            [utilities.schemas :as schemas :refer [message]]
            [service.gateway.util :refer [api-fn]]
            [utilities.auth :refer [authorization-middleware]]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def book-routes
  ["/books" {:swagger {:tags ["books"]}

             :middleware [authentication-middleware]}
   ["" {:get {:roles nil
              :middleware [authorization-middleware]
              :parameters {:query schemas/book-query}
              :responses {200 {:body [:map [:books [:sequential schemas/book-out]]]}}
              #_"TODO: check is it work when book-query is nil."
              :handler (api-fn [{book-query :query}]
                               (book-api/-get-all-books book-service book-query))}
        :post {:roles #{"admin"}
               :middleware [authorization-middleware]
               :parameters {:body schemas/book-add}
               :responses {201 {:body schemas/book-out
                                :headers {"Location" {:schema {:type "string"}}}}
                           422 {:body [:map
                                       [:type {:optional true} string?]
                                       [:message string?]]}}
               :handler (api-fn [{book :body}]
                                (book-api/-add-book book-service book))}}]
   ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

             :get {:roles nil
                   :middleware [authorization-middleware]
                   :responses {200 {:body schemas/book-out}
                               404 {:body message}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (book-api/-get-book book-service uid))}
             :delete {:roles #{"admin"}
                      :middleware [authorization-middleware]
                      :responses {200 {:body schemas/book-out}
                                  404 {:body message}
                                  422 {:body [:map
                                              [:type {:optional true} string?]
                                              [:message string?]]}
                                  500 {:body any?}
                                  502 {:body [:map
                                              [:response any?]
                                              [:message string?]]}}
                      :handler (api-fn [{{:keys [uid]} :path}]
                                       (book-api/-delete-book book-service uid))}
             :put {:roles #{"admin"}
                   :middleware [authorization-middleware]
                   :responses {200 {:body schemas/book-out}
                               404 {:body message}
                               500 {:body any?}
                               502 {:body [:map
                                           [:response any?]
                                           [:message string?]]}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (book-api/-restore-book book-service uid))}
             :patch {:roles #{"admin"}
                     :middleware [authorization-middleware]
                     :parameters {:body schemas/book-update}
                     :responses {200 {:body schemas/book-out}
                                 422 {:body [:map
                                             [:type {:optional true} string?]
                                             [:message string?]]}
                                 404 {:body message}}
                     :handler (api-fn [{{:keys [uid]} :path book :body}]
                                      (book-api/-update-book book-service uid book))}}]])
