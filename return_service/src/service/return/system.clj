(ns service.return.system
  (:require [org.httpkit.server :refer [run-server]]
            [service.return.router :refer [app]]
            [utilities.config :refer [load-config]]
            [service.return.tables.user-limit :as ul-ops
             :refer [->UserLimitTable UserLimitTableOperations]]
            [utilities.db.tables.client :as c-ops
             :refer [->ClientTable ClientTableOperations]]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [malli.core :as m]
            [utilities.core :refer [non-empty-string?]]
            [utilities.schemas :as schemas]
            [utilities.api.stats :refer [StatsAPI make-stats-service]])
  (:gen-class))

#_"ig/init-key"

(defmethod ig/init-key :service.return.system/db [_ {:keys [db-config]}]
  (let [user-limit-table (->UserLimitTable db-config)
        _ (ul-ops/-create user-limit-table)
        _ (ul-ops/-populate user-limit-table)
        client-table (->ClientTable db-config)
        _ (c-ops/-create client-table)
        _ (c-ops/-populate client-table)]
    {:tables {:user-limit user-limit-table
              :client client-table}}))

(defmethod ig/init-key :service.return.system/services
  [_ {:keys [rabbit-opts services-uri cb-options client-id client-secret]}]
  {:stats (make-stats-service (:stats services-uri) cb-options client-id client-secret rabbit-opts)})

(defmethod ig/init-key :service.return.system/app [_ {:keys [db services services-uri client-id]}]
  (app {:db db
        :services services
        :services-uri services-uri
        :client-id client-id}))

(defmethod ig/init-key :service.return.system/server [_ {:keys [app server-options]}]
  (run-server app server-options))

#_"ig/halt-key!"

(defmethod ig/halt-key! :service.return.system/server [_ server]
  (server :timeout 100))

#_"ig/pre-init-spec"

(s/def ::db-config (m/validator schemas/db-config))
(s/def ::db (m/validator
             [:map
              [:tables [:map
                        [:user-limit [:fn (fn [x] (satisfies? UserLimitTableOperations x))]]
                        [:client [:fn (fn [x] (satisfies? ClientTableOperations x))]]]]]))
(s/def ::services (m/validator [:map
                                [:stats [:fn (fn [x] (satisfies? StatsAPI x))]]]))
(s/def ::services-uri (m/validator schemas/services-uri))
(s/def ::cb-options (m/validator [:map
                                  [:failure-threshold-ratio [:tuple pos-int? pos-int?]]
                                  [:delay-ms nat-int?]]))
(s/def ::client-id non-empty-string?)
(s/def ::client-secret non-empty-string?)
(s/def ::app fn?)
(s/def ::server-options (m/validator schemas/server-options))
(s/def ::qname non-empty-string?)
(s/def ::amqp-url non-empty-string?)
(s/def ::rabbit-opts (s/keys :req-un [::qname ::amqp-url]))

(defmethod ig/pre-init-spec :service.return.system/db [_]
  (s/keys :req-un [::db-config]))

(defmethod ig/pre-init-spec :service.return.system/services [_]
  (s/keys :req-un [::rabbit-opts ::services-uri ::cb-options ::client-id ::client-secret]))

(defmethod ig/pre-init-spec :service.return.system/app [_]
  (s/keys :req-un [::db ::services ::services-uri ::client-id]))

(defmethod ig/pre-init-spec :service.return.system/server [_]
  (s/keys :req-un [::app ::server-options]))

#_"system init & halt!"

(defonce system (atom nil))

(defn start-system [config]
  (when @system (ig/halt! @system))
  (->> config
       (ig/init)
       (reset! system)))

(defn -main [profile & args]
  (->> {:profile (keyword profile)}
       (load-config "config.edn")
       (start-system)))

#_(-main "local")
#_(ig/halt! @system)
