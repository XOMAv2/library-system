(ns service.rating.system
  (:require [org.httpkit.server :refer [run-server]]
            [service.rating.router :refer [app]]
            [utilities.config :refer [load-config]]
            [service.rating.tables.user-rating :as ur-ops
             :refer [->UserRatingTable UserRatingTableOperations]]
            [utilities.db.tables.client :as c-ops
             :refer [->ClientTable ClientTableOperations]]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [malli.core :as m]
            [utilities.core :refer [non-empty-string?]]
            [utilities.schemas :as schemas]
            [utilities.api.stats :refer [StatsAPI map->StatsService]])
  (:gen-class))

#_"ig/init-key"

(defmethod ig/init-key :service.rating.system/db [_ {:keys [db-config]}]
  (let [user-rating-table (->UserRatingTable db-config)
        _ (ur-ops/-create user-rating-table)
        _ (ur-ops/-populate user-rating-table)
        client-table (->ClientTable db-config)
        _ (c-ops/-create client-table)
        _ (c-ops/-populate client-table)]
    {:tables {:user-rating user-rating-table
              :client client-table}}))

(defmethod ig/init-key :service.rating.system/services [_ {:keys [stats services-uri]}]
  {:stats (map->StatsService stats)})

(defmethod ig/init-key :service.rating.system/app [_ {:keys [db services services-uri]}]
  (app db services services-uri))

(defmethod ig/init-key :service.rating.system/server [_ {:keys [app server-options]}]
  (run-server app server-options))

#_"ig/halt-key!"

(defmethod ig/halt-key! :service.rating.system/server [_ server]
  (server :timeout 100))

#_"ig/pre-init-spec"

(s/def ::db-config (m/validator schemas/db-config))
(s/def ::db (m/validator
             [:map
              [:tables [:map
                        [:user-rating [:fn (fn [x] (satisfies? UserRatingTableOperations x))]]
                        [:client [:fn (fn [x] (satisfies? ClientTableOperations x))]]]]]))
(s/def ::services (m/validator [:map
                                [:stats [:fn (fn [x] (satisfies? StatsAPI x))]]]))
(s/def ::services-uri (m/validator schemas/services-uri))
(s/def ::app fn?)
(s/def ::server-options (m/validator schemas/server-options))
(s/def ::qname non-empty-string?)
(s/def ::amqp-url non-empty-string?)
(s/def ::stats (s/keys :req-un [::qname ::amqp-url]))

(defmethod ig/pre-init-spec :service.rating.system/db [_]
  (s/keys :req-un [::db-config]))

(defmethod ig/pre-init-spec :service.rating.system/services [_]
  (s/keys :req-un [::stats ::services-uri]))

(defmethod ig/pre-init-spec :service.rating.system/app [_]
  (s/keys :req-un [::db ::services ::services-uri]))

(defmethod ig/pre-init-spec :service.rating.system/server [_]
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
