(ns service.gateway.routes.stats
  (:require [utilities.api.stats :as stats-api]
            [utilities.schemas :as schemas :refer [message]]
            [service.gateway.util :refer [api-fn]]
            [utilities.auth :refer [authorization-middleware]]
            [service.gateway.middlewares :refer [authentication-middleware]]))

(def stats-routes
  ["/stats" {:swagger {:tags ["stats"]}

             :middleware [authentication-middleware]
             :roles #{"admin"}
             :middleware [authorization-middleware]}
   ["" {:get {:parameters {:query [:map [:service {:optional true} schemas/non-empty-string]]}
              :responses {200 {:body [:map [:stats [:sequential schemas/stat-record-out]]]}}
              #_"TODO: check is it work when service is nil."
              :handler (api-fn [{{:keys [service]} :query}]
                               (stats-api/-get-all-stat-records stats-service service))}
        :post {:parameters {:body schemas/stat-record-add}
               :responses {201 {:body schemas/stat-record-out
                                :headers {"Location" {:schema {:type "string"}}}}
                           422 {:body [:map
                                       [:type {:optional true} string?]
                                       [:message string?]]}}
               :handler (api-fn [{stat-record :body}]
                                (stats-api/-add-stat-record stats-service stat-record))}
        :delete {:responses {200 {:body [:map [:stats [:sequential schemas/stat-record-out]]]}}
                 :handler (api-fn []
                                  (stats-api/-delete-all-stat-records stats-service))}}]
   ["/:uid" {:parameters {:path [:map [:uid uuid?]]}

             :get {:responses {200 {:body schemas/stat-record-out}
                               404 {:body message}}
                   :handler (api-fn [{{:keys [uid]} :path}]
                                    (stats-api/-get-stat-record stats-service uid))}
             :delete {:responses {200 {:body schemas/stat-record-out}
                                  404 {:body message}}
                      :handler (api-fn [{{:keys [uid]} :path}]
                                       (stats-api/-delete-stat-record stats-service uid))}
             :patch {:parameters {:body schemas/stat-record-update}
                     :responses {200 {:body schemas/stat-record-out}
                                 422 {:body [:map
                                             [:type {:optional true} string?]
                                             [:message string?]]}
                                 404 {:body message}}
                     :handler (api-fn [{{:keys [uid]} :path stat-record :body}]
                                      (stats-api/-update-stat-record stats-service
                                                                     uid
                                                                     stat-record))}}]])
