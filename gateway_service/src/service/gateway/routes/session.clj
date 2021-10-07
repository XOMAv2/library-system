(ns service.gateway.routes.session
  (:require [utilities.api.session :as session-api]
            [utilities.schemas :as schemas :refer [message]]
            [service.gateway.util :refer [api-fn]]
            [utilities.auth :refer [authorization-middleware]]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def session-routes
  [""
   ["/users" {:swagger {:tags ["users"]}
              
              :middleware [authentication-middleware]}
    ["" {:get {:roles #{"admin"}
               :middleware [authorization-middleware]
               :responses {200 {:body [:map [:users [:sequential schemas/user-out]]]}}
               :handler (api-fn [nil :headers-dtor {a "authorization"}]
                                (session-api/-get-all-users session-service a))}
         :post {:parameters {:body schemas/user-add}
                :responses {201 {:body schemas/user-out-extended
                                 :headers {"Location" {:schema {:type "string"}}}}
                            422 {:body [:map
                                        [:type {:optional true} string?]
                                        [:message string?]]}
                            500 {:body any?}
                            502 {:body [:map
                                        [:response any?]
                                        [:message string?]]}}
                :handler (api-fn [{user :body}]
                                 (session-api/-add-user session-service user))}}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

              :get {:roles nil
                    :middleware [authorization-middleware]
                    :responses {200 {:body schemas/user-out-extended}
                                404 {:body message}}
                    :handler (api-fn [{{:keys [uid]} :path} :headers-dtor {a "authorization"}]
                                     (session-api/-get-user session-service a uid))}
              :delete {:roles #{"admin"}
                       :middleware [authorization-middleware]
                       :responses {200 {:body schemas/user-out}
                                   404 {:body message}
                                   422 {:body [:map
                                               [:type {:optional true} string?]
                                               [:message string?]]}
                                   500 {:body any?}
                                   502 {:body [:map
                                               [:response any?]
                                               [:message string?]]}}
                       :handler (api-fn [{{:keys [uid]} :path} :headers-dtor {a "authorization"}]
                                        (session-api/-delete-user session-service a uid))}
              :put {:roles #{"admin"}
                    :middleware [authorization-middleware]
                    :responses {200 {:body schemas/user-out}
                                404 {:body message}
                                500 {:body any?}
                                502 {:body [:map
                                            [:response any?]
                                            [:message string?]]}}
                    :handler (api-fn [{{:keys [uid]} :path} :headers-dtor {a "authorization"}]
                                     (session-api/-restore-user session-service a uid))}
              :patch {:roles nil
                      :middleware [authorization-middleware]
                      :parameters {:body schemas/user-update}
                      :responses {200 {:body schemas/user-out}
                                  422 {:body [:map
                                              [:type {:optional true} string?]
                                              [:message string?]]}
                                  404 {:body message}}
                      :handler (api-fn [{{:keys [uid]} :path user :body} :headers-dtor {a "authorization"}]
                                       (session-api/-update-user session-service a uid user))}}]]
   ["/auth" {:swagger {:tags ["auth"]}}
    ["/login" {:post {:parameters {:body [:map
                                          [:email schemas/non-empty-string]
                                          [:password schemas/non-empty-string]]}
                      :responses {200 {:body [:map
                                              [:tokens schemas/token-pair]
                                              [:payload [:map
                                                         [:uid uuid?]
                                                         [:role schemas/role]]]]}
                                  401 {:body message}
                                  404 {:body message}}
                      :handler (api-fn [{{:keys [email password]} :body}]
                                       (session-api/-get-tokens session-service email password))}}]
    ["/refresh" {:put {:parameters {:body [:map [:refresh-token schemas/non-empty-string]]}
                       :responses {200 {:body schemas/token-pair}
                                   401 {:body message}
                                   404 {:body message}}
                       :handler (api-fn [{{:keys [refresh-token]} :body}]
                                        (session-api/-refresh-tokens session-service
                                                                     refresh-token))}}]
    ["/verify" {:post {:parameters {:body [:map [:access-token schemas/non-empty-string]]}
                       :responses {200 {:body [:map
                                               [:uid uuid?]
                                               [:role schemas/role]
                                               [:exp nat-int?]]}
                                   401 {:body message}}
                       :handler
                       (api-fn [{{:keys [access-token]} :body}]
                               (session-api/-verify-token session-service access-token))}}]]])
