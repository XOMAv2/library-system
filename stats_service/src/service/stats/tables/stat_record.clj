(ns service.stats.tables.stat-record
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            #_[next.jdbc.date-time]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :refer [add-entity get-entity get-all-entities get-all-entities-by-keys
                                  update-entity delete-entity delete-all-entities
                                  create-table populate-table
                                  jdbc-opts]]))

(defprotocol StatRecordTableOperations
  (-create [this]
    "Returns nil if table is created or already exists, throws exception otherwise.")
  (-populate [this]
    "Populates empty table with default data and returns number of added rows.
     Returns nil if table isn't empty.")
  (-add [this entity]
    "Returns entity if it's added.
     Throws exception if entity is malformed.")
  (-get [this id]
    "Returns entity if it's found, returns nil otherwise.")
  (-get-all [this]
    "Returns collection of entities if table isn't empty, returns empty collection otherwise.")
  (-get-all-by-service [this service]
    "Returns collection of entities for specified service.
     Returns empty collection if service contains no entities or service wasn't found.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise.")
  (-delete-all [this]
    "Returns collection of deleted entities if table isn't empty,
     returns empty collection otherwise."))

(def ^:private tname :stat_record)

(def ^:private sanitize
  (m/decoder schemas/stat-record-out mt/strip-extra-keys-transformer))

(defrecord StatRecordTable [db]
  StatRecordTableOperations
  (-create [this]
    (create-table db tname ["id           int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                            "uid          uuid NOT NULL UNIQUE"
                            "service      text NOT NULL"
                            "operation    text NOT NULL"
                            "send_time    timestamp NOT NULL"
                            "receive_time timestamp NOT NULL"]))
  (-populate [this]
    (populate-table db tname []))
  (-add [this entity]
    (add-entity db tname entity sanitize))
  (-get [this id]
    (get-entity db tname id sanitize))
  (-get-all [this]
    (get-all-entities db tname sanitize))
  (-get-all-by-service [this service]
    (get-all-entities-by-keys db tname {:service service} sanitize))
  (-update [this id entity]
    (update-entity db tname id entity sanitize))
  (-delete [this id]
    (delete-entity db tname id sanitize))
  (-delete-all [this]
    (delete-all-entities db tname sanitize)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def config
    (load-config "config.edn" {:profile :local}))

  (def db-config
    (get-in config [:service.stats.system/db :db-config]))
  
  (def db
    (jdbc/get-datasource db-config))

  (def stat-record-table
    (->StatRecordTable db))
  
  (add-entity db "stat_record" {:service "book"
                                :operation "get"
                                :send-time #inst "2020-11-09"})

  (-get-all stat-record-table)
  (-delete-all stat-record-table)
  (-get-all-by-service stat-record-table "book")

  (-add stat-record-table {:service "book"
                           :operation "get"
                           :send-time #inst "2020-11-09"
                           :receive-time #inst "2020-11-09"})
  )
