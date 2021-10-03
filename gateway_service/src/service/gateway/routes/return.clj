(ns service.gateway.routes.return
  (:require [utilities.api.return :as return-api]
            [utilities.schemas :as schemas :refer [message]]
            [service.gateway.util :refer [api-fn]]
            [utilities.auth :refer [authorization-middleware]]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def return-routes
  ["/limits" {:swagger {:tags ["limits"]}
              
              :middleware [authentication-middleware]}
   ["" {:get {:roles nil
              :middleware [authorization-middleware]
              :responses {200 {:body [:map [:limits [:sequential schemas/user-limit-out]]]}}
              :handler (api-fn []
                               (return-api/-get-all-user-limits return-service))}
        :post {:roles #{"admin"}
               :middleware [authorization-middleware]
               :parameters {:body schemas/user-limit-add}
               :responses {201 {:body schemas/user-limit-out
                                :headers {"Location" {:schema {:type "string"}}}}
                           422 {:body [:map
                                       [:type {:optional true} string?]
                                       [:message string?]]}}
               :handler (api-fn [{user-limit :body}]
                                (return-api/-add-user-limit return-service user-limit))}}]
   ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

             :get {:roles nil
                   :middleware [authorization-middleware]
                   :responses {200 {:body schemas/user-limit-out}
                               404 {:body message}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (return-api/-get-user-limit return-service uid))}
             :delete {:roles #{"admin"}
                      :middleware [authorization-middleware]
                      :responses {200 {:body schemas/user-limit-out}
                                  404 {:body message}}
                      :handler (api-fn [{{:keys [uid]} :path}]
                                       (return-api/-delete-user-limit return-service uid))}
             :put {:roles #{"admin"}
                   :middleware [authorization-middleware]
                   :responses {200 {:body schemas/user-limit-out}
                               404 {:body message}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (return-api/-restore-user-limit return-service uid))}
             :patch {:roles #{"admin"}
                     :middleware [authorization-middleware]
                     :parameters {:body schemas/user-limit-update}
                     :responses {200 {:body schemas/user-limit-out}
                                 422 {:body [:map
                                             [:type {:optional true} string?]
                                             [:message string?]]}
                                 404 {:body message}}
                     :handler (api-fn [{{:keys [uid]} :path user-limit :body}]
                                      (return-api/-update-user-limit return-service
                                                                     uid
                                                                     user-limit))}}]
   ["/user-uid/:user-uid" {:get {:roles nil
                                 :middleware [authorization-middleware]
                                 :parameters {:path [:map [:user-uid uuid?]]}
                                 :responses {200 {:body schemas/user-limit-out}
                                             404 {:body message}}
                                 :handler (api-fn [{{:keys [user-uid]} :path}]
                                                  (return-api/-get-user-limit-by-user-uid return-service
                                                                                          user-uid))}
                           :delete {:roles #{"admin"}
                                    :middleware [authorization-middleware]
                                    :responses {200 {:body schemas/user-limit-out}
                                                404 {:body message}}
                                    :handler (api-fn [{{:keys [user-uid]} :path}]
                                                     (return-api/-delete-user-limit-by-user-uid return-service
                                                                                                user-uid))}
                           :put {:roles #{"admin"}
                                 :middleware [authorization-middleware]
                                 :responses {200 {:body schemas/user-limit-out}
                                             404 {:body message}}
                                 :handler (api-fn [{{:keys [user-uid]} :path}]
                                                  (return-api/-restore-user-limit-by-user-uid return-service
                                                                                              user-uid))}}]
   ["/user-uid/:user-uid/total-limit/:value"
    {:roles #{"admin"}
     :middleware [authorization-middleware]

     :post {:parameters {:path [:map
                                [:user-uid uuid?]
                                [:value nat-int?]]}
            :responses {200 {:body schemas/user-limit-out}
                        422 {:body [:map
                                    [:type {:optional true} string?]
                                    [:message string?]]}
                        404 {:body message}}
            :handler (api-fn [{{:keys [user-uid value]} :path}]
                             (return-api/-reset-total-limit-by-user-uid return-service
                                                                        user-uid
                                                                        value))}
     :patch {:parameters {:path [:map
                                 [:user-uid uuid?]
                                 [:value int?]]}
             :responses {200 {:body schemas/user-limit-out}
                         422 {:body [:map
                                     [:type {:optional true} string?]
                                     [:message string?]]}
                         404 {:body message}}
             :handler (api-fn [{{user-uid :user-uid delta :value} :path}]
                              (return-api/-update-total-limit-by-user-uid return-service
                                                                          user-uid
                                                                          delta))}}]
   ["/user-uid/:user-uid/available-limit/:delta"
    {:patch {:roles #{"admin"}
             :middleware [authorization-middleware]
             :parameters {:path [:map
                                 [:user-uid uuid?]
                                 [:delta int?]]}
             :responses {200 {:body schemas/user-limit-out}
                         422 {:body [:map
                                     [:type {:optional true} string?]
                                     [:message string?]]}
                         404 {:body message}}
             :handler (api-fn [{{:keys [user-uid delta]} :path}]
                              (return-api/-update-available-limit-by-user-uid return-service
                                                                              user-uid
                                                                              delta))}}]])
