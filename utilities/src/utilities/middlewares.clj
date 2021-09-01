(ns utilities.middlewares
  (:require [utilities.api.stats :refer [StatsAPI -add-stat-record]]
            [clojure.spec.alpha :as s]
            [utilities.time :as time]
            [utilities.core :refer [non-empty-string?]]))

(s/def ::stats (partial satisfies? StatsAPI))
(s/def ::services (s/keys :req-un [::stats]))
(s/def :stast/service non-empty-string?)

(def request->stats-middleware
  {:name ::request->stats-middleware
   :spec (s/keys :req-un [::services] :req [:stats/service])
   :compile (fn [{{:keys [stats]} :services
                  service         :stats/service} _]
              (fn [handler]
                (fn [request]
                  (let [request (assoc request :uid (java.util.UUID/randomUUID))
                        operation (select-keys request [:uid :request-method
                                                        :uri :headers :parameters])
                        stat-record {:service service
                                     :operation (str operation)
                                     :send-time (time/now)}]
                    (-add-stat-record stats stat-record)
                    (handler request)))))})

(def response->stats-middleware
  {:name ::response->stats-middleware
   :spec (s/keys :req-un [::services] :req [:stats/service])
   :compile (fn [{{:keys [stats]} :services
                  service         :stats/service} _]
              (fn [handler]
                (fn [request]
                  (let [response (handler request)
                        operation (merge (select-keys request [:uri])
                                         (select-keys response [:status]))
                        stat-record {:service service
                                     :operation (str operation)
                                     :send-time (time/now)}]
                    (-add-stat-record stats stat-record)
                    response))))})
