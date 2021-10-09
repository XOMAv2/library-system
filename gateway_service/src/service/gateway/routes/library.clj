(ns service.gateway.routes.library
  (:require [utilities.api.library :as library-api]
            [utilities.schemas :as schemas :refer [message]]
            [service.gateway.util :refer [api-fn]]
            [utilities.auth :refer [authorization-middleware]]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def library-routes
  [""
   ["/libraries" {:swagger {:tags ["libraries"]}
                  
                  :middleware [authentication-middleware]}
    ["" {:get {:roles nil
               :middleware [authorization-middleware]
               :parameters {:query schemas/library-query}
               :responses {200 {:body [:map [:libraries [:sequential schemas/library-out]]]}}
               #_"TODO: check is it work when library-query is nil."
               :handler (api-fn [{library-query :query}]
                                (library-api/-get-all-libraries library-service library-query))}
         :post {:roles #{"admin"}
                :middleware [authorization-middleware]
                :parameters {:body schemas/library-add}
                :responses {201 {:body schemas/library-out
                                 :headers {"Location" {:schema {:type "string"}}}}
                            422 {:body [:map
                                        [:type {:optional true} string?]
                                        [:message string?]]}}
                :handler (api-fn [{library :body}]
                                 (library-api/-add-library library-service library))}}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

              :get {:roles nil
                    :middleware [authorization-middleware]
                    :responses {200 {:body schemas/library-out}
                                404 {:body message}}
                    :handler (api-fn [{{:keys [uid]} :path}]
                                     (library-api/-get-library library-service uid))}
              :delete {:roles #{"admin"}
                       :middleware [authorization-middleware]
                       :responses {200 {:body schemas/library-out}
                                   404 {:body message}}
                       :handler (api-fn [{{:keys [uid]} :path}]
                                        (library-api/-delete-library library-service uid))}
              :patch {:roles #{"admin"}
                      :middleware [authorization-middleware]
                      :parameters {:body schemas/library-update}
                      :responses {200 {:body schemas/library-out}
                                  422 {:body [:map
                                              [:type {:optional true} string?]
                                              [:message string?]]}
                                  404 {:body message}}
                      :handler (api-fn [{{:keys [uid]} :path library :body}]
                                       (library-api/-update-library library-service uid library))}}]]

   ["/library-books" {:swagger {:tags ["library-books"]}}
    ["" {:get {:roles nil
               :middleware [authorization-middleware]
               :parameters {:query schemas/library-book-query}
               :responses {200 {:body [:map [:library-books [:sequential schemas/library-book-out]]]}}
               #_"TODO: check is it work when library-book-query is nil."
               :handler (api-fn [{library-book-query :query}]
                                (library-api/-get-all-library-books library-service
                                                                    library-book-query))}
         :post {:roles #{"admin"}
                :middleware [authorization-middleware]
                :parameters {:body schemas/library-book-add}
                :responses {201 {:body schemas/library-book-out
                                 :headers {"Location" {:schema {:type "string"}}}}
                            422 {:body [:map
                                        [:type {:optional true} string?]
                                        [:message string?]]}}
                :handler (api-fn [{library-book :body}]
                                 (library-api/-add-library-book library-service library-book))}
         :delete {:roles #{"admin"}
                  :middleware [authorization-middleware]
                  :parameters {:query schemas/library-book-query}
                  :responses {200 {:body [:map [:library-books [:sequential schemas/library-book-out]]]}}
                  :handler (api-fn [{library-book-query :query}]
                                   (library-api/-delete-all-library-books library-service
                                                                          library-book-query))}
         :put {:roles #{"admin"}
               :middleware [authorization-middleware]
               :parameters {:query schemas/library-book-query}
               :responses {200 {:body [:map [:library-books [:sequential schemas/library-book-out]]]}}
               :handler (api-fn [{library-book-query :query}]
                                (library-api/-restore-all-library-books library-service
                                                                        library-book-query))}}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

              :get {:roles nil
                    :middleware [authorization-middleware]
                    :responses {200 {:body schemas/library-book-out}
                                404 {:body message}}
                    :handler (api-fn [{{:keys [uid]} :path}]
                                     (library-api/-get-library-book library-service uid))}
              :delete {:roles #{"admin"}
                       :middleware [authorization-middleware]
                       :responses {200 {:body schemas/library-book-out}
                                   404 {:body message}}
                       :handler (api-fn [{{:keys [uid]} :path}]
                                        (library-api/-delete-library-book library-service uid))}
              :put {:roles #{"admin"}
                    :middleware [authorization-middleware]
                    :responses {200 {:body schemas/library-book-out}
                                404 {:body message}}
                    :handler (api-fn [{{:keys [uid]} :path}]
                                     (library-api/-restore-library-book library-service uid))}
              :patch {:roles #{"admin"}
                      :middleware [authorization-middleware]
                      :parameters {:body schemas/library-book-update}
                      :responses {200 {:body schemas/library-book-out}
                                  422 {:body [:map
                                              [:type {:optional true} string?]
                                              [:message string?]]}
                                  404 {:body message}}
                      :handler (api-fn [{{:keys [uid]} :path library-book :body}]
                                       (library-api/-update-library-book library-service
                                                                         uid
                                                                         library-book))}}]]

   ["/orders" {:swagger {:tags ["orders"]}}
    ["" {:get {:roles nil
               :middleware [authorization-middleware]
               :parameters {:query schemas/order-query}
               :responses {200 {:body [:map [:orders [:sequential schemas/order-out]]]}}
               #_"TODO: check is it work when order-query is nil."
               :handler (api-fn [{order-query :query}]
                                (library-api/-get-all-orders library-service order-query))}
         :post {:roles nil
                :middleware [authorization-middleware]
                :parameters {:body schemas/order-add}
                :responses {201 {:body schemas/order-out
                                 :headers {"Location" {:schema {:type "string"}}}}
                            404 {:body message}
                            422 {:body [:map
                                        [:type {:optional true} string?]
                                        [:message string?]]}
                            500 {:body any?}
                            502 {:body [:map
                                        [:response any?]
                                        [:message string?]]}}
                :handler (api-fn [{order :body}]
                                 (library-api/-add-order library-service order))}
         :patch {:roles #{"admin"}
                 :middleware [authorization-middleware]
                 :parameters {:query schemas/order-query
                              :body schemas/order-update}
                 :responses {200 {:body [:map [:orders [:sequential schemas/order-out]]]}
                             422 {:body [:map
                                         [:type {:optional true} string?]
                                         [:message string?]]}}
                 :handler (api-fn [{order-query :query order :body}]
                                  (library-api/-update-all-orders library-service
                                                                  order-query
                                                                  order))}}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

              :get {:roles nil
                    :middleware [authorization-middleware]
                    :responses {200 {:body schemas/order-out-extended}
                                404 {:body message}}
                    :handler (api-fn [{{:keys [uid]} :path}]
                                     (library-api/-get-order library-service uid))}
              :delete {:roles #{"admin"}
                       :middleware [authorization-middleware]
                       :responses {200 {:body schemas/order-out}
                                   404 {:body message}}
                       :handler (api-fn [{{:keys [uid]} :path}]
                                        (library-api/-delete-order library-service uid))}
              :patch {:roles #{"admin"}
                      :middleware [authorization-middleware]
                      :parameters {:body schemas/order-update}
                      :responses {200 {:body schemas/order-out}
                                  404 {:body message}
                                  422 {:body [:map
                                              [:type {:optional true} string?]
                                              [:message string?]]}
                                  500 {:body any?}
                                  502 {:body [:map
                                              [:response any?]
                                              [:message string?]]}}
                      :handler (api-fn [{{:keys [uid]} :path order :body}]
                                       (library-api/-update-order library-service uid order))}}]]])
