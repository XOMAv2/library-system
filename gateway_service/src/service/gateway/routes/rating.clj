(ns service.gateway.routes.rating
  (:require [utilities.api.rating :as rating-api]
            [utilities.schemas :as schemas :refer [message]]
            [service.gateway.util :refer [api-fn]]
            [utilities.auth :refer [authorization-middleware]]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def rating-routes
  ["/ratings" {:swagger {:tags ["ratings"]}
               
               :middleware [authentication-middleware]}
   ["" {:roles #{"admin"}
        :middleware [authorization-middleware]

        :get {:responses {200 {:body [:map [:ratings [:sequential schemas/user-rating-out]]]}}
              :handler (api-fn []
                               (rating-api/-get-all-user-ratings rating-service))}
        :post {:parameters {:body schemas/user-rating-add}
               :responses {201 {:body schemas/user-rating-out
                                :headers {"Location" {:schema {:type "string"}}}}
                           422 {:body [:map
                                       [:type {:optional true} string?]
                                       [:message string?]]}
                           500 {:body any?}
                           502 {:body [:map
                                       [:response any?]
                                       [:message string?]]}}
               :handler (api-fn [{user-rating :body}]
                                (rating-api/-add-user-rating rating-service user-rating))}}]
   ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

             :get {:roles nil
                   :middleware [authorization-middleware]
                   :responses {200 {:body schemas/user-rating-out}
                               404 {:body message}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (rating-api/-get-user-rating rating-service uid))}
             :delete {:roles #{"admin"}
                      :middleware [authorization-middleware]
                      :responses {200 {:body schemas/user-rating-out}
                                  404 {:body message}
                                  500 {:body any?}
                                  502 {:body [:map
                                              [:response any?]
                                              [:message string?]]}}
                      :handler (api-fn [{{:keys [uid]} :path}]
                                       (rating-api/-delete-user-rating rating-service uid))}
             :put {:roles #{"admin"}
                   :middleware [authorization-middleware]
                   :responses {200 {:body schemas/user-rating-out}
                               404 {:body message}
                               500 {:body any?}
                               502 {:body [:map
                                           [:response any?]
                                           [:message string?]]}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (rating-api/-restore-user-rating rating-service uid))}
             :patch {:roles #{"admin"}
                     :middleware [authorization-middleware]
                     :parameters {:body schemas/user-rating-update}
                     :responses {200 {:body schemas/user-rating-out}
                                 422 {:body [:map
                                             [:type {:optional true} string?]
                                             [:message string?]]}
                                 404 {:body message}
                                 500 {:body any?}
                                 502 {:body [:map
                                             [:response any?]
                                             [:message string?]]}}
                     :handler (api-fn [{{:keys [uid]} :path user-rating :body}]
                                      (rating-api/-update-user-rating rating-service
                                                                      uid
                                                                      user-rating))}}]
   ["/user-uid/:user-uid" {:get {:roles nil
                                 :middleware [authorization-middleware]
                                 :parameters {:path [:map [:user-uid uuid?]]}
                                 :responses {200 {:body schemas/user-rating-out}
                                             404 {:body message}}
                                 :handler (api-fn [{{:keys [user-uid]} :path}]
                                                  (rating-api/-get-user-rating-by-user-uid rating-service
                                                                                           user-uid))}
                           :delete {:roles #{"admin"}
                                    :middleware [authorization-middleware]
                                    :responses {200 {:body schemas/user-rating-out}
                                                404 {:body message}
                                                500 {:body any?}
                                                502 {:body [:map
                                                            [:response any?]
                                                            [:message string?]]}}
                                    :handler (api-fn [{{:keys [user-uid]} :path}]
                                                     (rating-api/-delete-user-rating-by-user-uid rating-service
                                                                                                 user-uid))}
                           :put {:roles #{"admin"}
                                 :middleware [authorization-middleware]
                                 :responses {200 {:body schemas/user-rating-out}
                                             404 {:body message}
                                             500 {:body any?}
                                             502 {:body [:map
                                                         [:response any?]
                                                         [:message string?]]}}
                                 :handler (api-fn [{{:keys [user-uid]} :path}]
                                                  (rating-api/-restore-user-rating-by-user-uid rating-service
                                                                                               user-uid))}}]
   ["/user-uid/:user-uid/rating/:condition"
    {:patch {:roles #{"admin"}
             :middleware [authorization-middleware]
             :parameters {:path [:map
                                 [:user-uid uuid?]
                                 [:condition schemas/condition]]}
             :responses {200 {:body schemas/user-rating-out}
                         404 {:body message}
                         422 {:body [:map
                                     [:type {:optional true} string?]
                                     [:message string?]]}
                         500 {:body any?}
                         502 {:body [:map
                                     [:response any?]
                                     [:message string?]]}}
             :handler (api-fn [{{:keys [user-uid condition]} :path}]
                              (rating-api/-update-rating-by-user-uid rating-service
                                                                     user-uid
                                                                     condition))}}]])
