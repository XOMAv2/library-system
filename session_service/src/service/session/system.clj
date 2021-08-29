(ns service.session.system
  (:require [org.httpkit.server :refer [run-server]]
            [service.session.router :refer [app]]
            [utilities.core :refer [load-config]]
            [service.session.tables.user :refer [->UserTable -create -populate
                                                 UserTableOperations]]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [malli.core :as m]
            [utilities.schemas :as schemas])
  (:gen-class))

#_"ig/init-key"

(defmethod ig/init-key :service.session.system/db [_ {:keys [db-config]}]
  (let [user-table (->UserTable db-config)
        _ (-create user-table)
        _ (-populate user-table)]
    {:tables {:user user-table}}))

(defmethod ig/init-key :service.session.system/app [_ {:keys [db services-uri]}]
  (app db services-uri))

(defmethod ig/init-key :service.session.system/server [_ {:keys [app server-options]}]
  (run-server app server-options))

#_"ig/halt-key!"

(defmethod ig/halt-key! :service.session.system/server [_ server]
  (server :timeout 100))

#_"ig/pre-init-spec"

(s/def ::db-config (m/validator schemas/db-config))
(s/def ::db (m/validator [:map
                          [:tables [:map
                                    [:user [:fn (fn [x] (satisfies? UserTableOperations x))]]]]]))
(s/def ::services-uri (m/validator schemas/services-uri))
(s/def ::app fn?)
(s/def ::server-options (m/validator schemas/server-options))

(defmethod ig/pre-init-spec :service.session.system/db [_]
  (s/keys :req-un [::db-config]))

(defmethod ig/pre-init-spec :service.session.system/app [_]
  (s/keys :req-un [::db ::services-uri]))

(defmethod ig/pre-init-spec :service.session.system/server [_]
  (s/keys :req-un [::app ::server-options]))

#_"system init & halt!"

(defonce system (atom nil))

(defn -main [profile & args]
  (->> {:profile (keyword profile)}
       (load-config "config.edn")
       (ig/init)
       (reset! system)))

#_(-main "local")
#_(ig/halt! @system)
