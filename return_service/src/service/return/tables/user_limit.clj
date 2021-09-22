(ns service.return.tables.user-limit
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db.core :as udb]
            [utilities.db.crud.soft :as crud]
            [honey.sql :as h]
            [clojure.math.numeric-tower :refer [abs]]))

(defprotocol UserLimitTableOperations
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
  (-get-by-user-uid [this user-uid]
    "Returns entity if it's found, returns nil otherwise.")
  (-get-all [this]
    "Returns collection of entities if table isn't empty, returns empty collection otherwise.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-update-total-limit-by-user-uid [this user-uid delta]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-update-available-limit-by-user-uid [this user-uid delta]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise.")
  (-delete-by-user-uid [this user-uid]
    "")
  (-restore [this id]
    "")
  (-restore-by-user-uid [this user-uid]
    ""))

(def ^:private tname :user_limit)

(def ^:private sanitize
  (m/decoder schemas/user-limit-out mt/strip-extra-keys-transformer))

(defrecord UserLimitTable [db]
  UserLimitTableOperations
  (-create [this]
    (udb/create-table
     db tname
     ["id              int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
      "uid             uuid NOT NULL UNIQUE"
      "user_uid        uuid NOT NULL UNIQUE"
      #_"Максимальное число книг, доступных пользователю для бронирования/получения на руки."
      "total_limit     int NOT NULL CHECK (total_limit >= 0)"
      #_"Число книг, доступное пользователю в данный момент для бронирования/получения."
      "available_limit int NOT NULL CHECK (available_limit <= total_limit)"
      "is_deleted      boolean NOT NULL"]))
  (-populate [this]
    (udb/populate-table db tname [] :delete-mode :soft))
  (-add [this entity]
    (crud/add-entity db tname entity sanitize))
  (-get [this id]
    (crud/get-entity db tname id sanitize))
  (-get-by-user-uid [this user-uid]
    (crud/get-entity-by-keys db tname {:user_uid user-uid} sanitize))
  (-get-all [this]
    (crud/get-all-entities db tname sanitize))
  (-update [this id entity]
    (crud/update-entity db tname id entity sanitize))
  (-update-total-limit-by-user-uid [this user-uid delta]
    (let [sign (if (neg? delta) :- :+)
          query (h/format {:update tname
                           :set {:total-limit [sign :total-limit (abs delta)]
                                 :available-limit [sign :available-limit (abs delta)]}
                           :where [:and
                                   [:= :is-deleted false]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize))))
  (-update-available-limit-by-user-uid [this user-uid delta]
    (let [sign (if (neg? delta) :- :+)
          query (h/format {:update tname
                           :set {:available-limit [sign :available-limit (abs delta)]}
                           :where [:and
                                   [:= :is-deleted false]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize))))
  (-delete [this id]
    (crud/delete-entity db tname id sanitize))
  (-delete-by-user-uid [this user-uid]
    (let [query (h/format {:update tname
                           :set {:is-deleted true}
                           :where [:and
                                   [:= :is-deleted false]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize))))
  (-restore [this id]
    (crud/restore-entity db tname id sanitize))
  (-restore-by-user-uid [this user-uid]
    (let [query (h/format {:update tname
                           :set {:is-deleted false}
                           :where [:and
                                   [:= :is-deleted true]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize)))))

(comment
  (require '[utilities.config :refer [load-config]])

  (def config
    (load-config "config.edn" {:profile :local}))

  (def db-config
    (get-in config [:service.return.system/db :db-config]))

  (def db
    (jdbc/get-datasource db-config))

  (def user-limit-table
    (->UserLimitTable db))

  (-create user-limit-table)

  (-get-by-user-uid user-limit-table #uuid "fcffadfb-a200-4165-9ca9-0010530ee970")

  (-get-all user-limit-table)

  (-add user-limit-table {:user_uid (java.util.UUID/randomUUID)
                          :total_limit 54
                          :available_limit 20}))
