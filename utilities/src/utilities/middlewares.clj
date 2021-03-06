(ns utilities.middlewares
  (:require [utilities.api.stats :refer [StatsAPI -add-stat-record-queue]]
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
                        body (select-keys request [:uid :request-method
                                                   :uri :headers :parameters])
                        stat-record {:service service
                                     :body (str body)
                                     :content-type "application/edn; charset=utf-8"
                                     :send-time (time/now)}]
                    (-add-stat-record-queue stats stat-record)
                    (handler request)))))})

(def response->stats-middleware
  {:name ::response->stats-middleware
   :spec (s/keys :req-un [::services] :req [:stats/service])
   :compile (fn [{{:keys [stats]} :services
                  service         :stats/service} _]
              (fn [handler]
                (fn [request]
                  (let [response (handler request)
                        body (merge (select-keys request [:uid])
                                    (select-keys response [:status]))
                        stat-record {:service service
                                     :body (str body)
                                     :content-type "application/edn; charset=utf-8"
                                     :send-time (time/now)}]
                    (-add-stat-record-queue stats stat-record)
                    response))))})

(defn key->request
  "Adds a slot to the request using the assoc function."
  [handler key value]
  (fn [request]
    (-> request
        (assoc key value)
        (handler))))

(defn generated-key->request
  "`generator` - function without arguments."
  [handler key generator]
  (fn [request]
    (-> request
        (assoc key (generator))
        (handler))))
