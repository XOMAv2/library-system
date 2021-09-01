(ns service.stats.system
  (:require [org.httpkit.server :refer [run-server]]
            [service.stats.router :refer [app]]
            [utilities.config :refer [load-config]]
            [service.stats.tables.stat-record :as stops
             :refer [->StatRecordTable StatRecordTableOperations]]
            [utilities.tables.client :as cops
             :refer [->ClientTable ClientTableOperations]]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [malli.core :as m]
            [utilities.schemas :as schemas]
            [service.stats.handlers :as handlers]
            [utilities.core :refer [non-empty-string?]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.consumers :as lc])
  (:gen-class))

#_"ig/init-key"

(defmethod ig/init-key :service.stats.system/db [_ {:keys [db-config]}]
  (let [stat-record-table (->StatRecordTable db-config)
        _ (stops/-create stat-record-table)
        _ (stops/-populate stat-record-table)
        client-table (->ClientTable db-config)
        _ (cops/-create client-table)
        _ (cops/-populate client-table)]
    {:tables {:stat-record stat-record-table
              :client client-table}}))

(defmethod ig/init-key :service.stats.system/app [_ {:keys [db services-uri]}]
  (app db services-uri))

(defmethod ig/init-key :service.stats.system/server [_ {:keys [app server-options]}]
  (run-server app server-options))

(defmethod ig/init-key :service.stats.system/rabbitmq [_ {:keys [db qname amqp-url services-uri]}]
  (let [add-stat-record-form-queue
        (fn [ch {:keys [content-type] :as meta} ^bytes payload]
          (when (= content-type "application/edn; charset=utf-8")
            (try (let [stat-record (-> payload
                                       (String. "UTF-8")
                                       (read-string))]
                   (when (m/validate schemas/stat-record-add stat-record)
                     (handlers/add-stat-record {:parameters {:body stat-record}
                                                :db db
                                                :services-uri services-uri})))
                 (catch Exception _))))

        conn (rmq/connect {:uri amqp-url})
        ch (lch/open conn)]
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lc/subscribe ch qname add-stat-record-form-queue {:auto-ack true})
    {:channel ch
     :connection conn}))

#_"ig/halt-key!"

(defmethod ig/halt-key! :service.stats.system/server [_ server]
  (server :timeout 100))

(defmethod ig/halt-key! :service.stats.system/rabbitmq [_ {:keys [channel connection] :as rabbitmq}]
  (when (rmq/open? channel)
    (rmq/close channel))
  (when (rmq/open? connection)
    (rmq/close connection)))

#_"ig/pre-init-spec"

(s/def ::db-config (m/validator schemas/db-config))
(s/def ::db (m/validator
             [:map
              [:tables [:map
                        [:stat-record [:fn (fn [x] (satisfies? StatRecordTableOperations x))]]
                        [:client [:fn (fn [x] (satisfies? ClientTableOperations x))]]]]]))
(s/def ::services-uri (m/validator schemas/services-uri))
(s/def ::app fn?)
(s/def ::server-options (m/validator schemas/server-options))
(s/def ::qname non-empty-string?)
(s/def ::amqp-url non-empty-string?)

(defmethod ig/pre-init-spec :service.stats.system/db [_]
  (s/keys :req-un [::db-config]))

(defmethod ig/pre-init-spec :service.stats.system/app [_]
  (s/keys :req-un [::db ::services-uri]))

(defmethod ig/pre-init-spec :service.stats.system/server [_]
  (s/keys :req-un [::app ::server-options]))

(defmethod ig/pre-init-spec :service.stats.system/rabbitmq [_]
  (s/keys :req-un [::db ::services-uri ::qname ::amqp-url]))

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
