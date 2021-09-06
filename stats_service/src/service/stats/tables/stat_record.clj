(ns service.stats.tables.stat-record
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            #_[next.jdbc.date-time]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :as udb]))

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
    (udb/create-table db tname ["id           int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                                "uid          uuid NOT NULL UNIQUE"
                                "service      text NOT NULL"
                                "body         text NOT NULL"
                                "content_type text NOT NULL"
                                "send_time    timestamp NOT NULL"
                                "receive_time timestamp NOT NULL"]))
  (-populate [this]
    (udb/populate-table db tname []))
  (-add [this entity]
    (udb/add-entity db tname entity sanitize))
  (-get [this id]
    (udb/get-entity db tname id sanitize))
  (-get-all [this]
    (udb/get-all-entities db tname sanitize))
  (-get-all-by-service [this service]
    (udb/get-all-entities-by-keys db tname {:service service} sanitize))
  (-update [this id entity]
    (udb/update-entity db tname id entity sanitize))
  (-delete [this id]
    (udb/delete-entity db tname id sanitize))
  (-delete-all [this]
    (udb/delete-all-entities db tname sanitize)))

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
  
  (udb/add-entity db "stat_record" {:service "book"
                                    :body "get"
                                    :content-type "text/plain"
                                    :send-time #inst "2020-11-09"})

  (-get-all stat-record-table)
  (-delete-all stat-record-table)
  (-get-all-by-service stat-record-table "book")

  (-add stat-record-table {:service "book"
                           :body "get"
                           :content-type "text/plain"
                           :send-time #inst "2020-11-09"
                           :receive-time #inst "2020-11-09"})
  )
