(ns service.return.system
  (:require [org.httpkit.server :refer [run-server]]
            [service.return.router :refer [app]]
            [utilities.config :refer [load-config]]
            [service.return.tables.user-limit :as ulops
             :refer [->UserLimitTable UserLimitTableOperations]]
            [utilities.tables.client :as cops
             :refer [->ClientTable ClientTableOperations]]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [malli.core :as m]
            [utilities.schemas :as schemas])
  (:gen-class))

#_"ig/init-key"

(defmethod ig/init-key :service.return.system/db [_ {:keys [db-config]}]
  (let [user-limit-table (->UserLimitTable db-config)
        _ (ulops/-create user-limit-table)
        _ (ulops/-populate user-limit-table)
        client-table (->ClientTable db-config)
        _ (cops/-create client-table)
        _ (cops/-populate client-table)]
    {:tables {:user-limit user-limit-table
              :client client-table}}))

(defmethod ig/init-key :service.return.system/app [_ {:keys [db services-uri]}]
  (app db services-uri))

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
(s/def ::services-uri (m/validator schemas/services-uri))
(s/def ::app fn?)
(s/def ::server-options (m/validator schemas/server-options))

(defmethod ig/pre-init-spec :service.return.system/db [_]
  (s/keys :req-un [::db-config]))

(defmethod ig/pre-init-spec :service.return.system/app [_]
  (s/keys :req-un [::db ::services-uri]))

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
