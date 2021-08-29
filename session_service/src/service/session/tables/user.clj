(ns service.session.tables.user
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :refer [add-entity get-entity get-all-entities
                                  update-entity delete-entity
                                  create-table populate-table
                                  jdbc-opts]]))

(defprotocol UserTableOperations
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
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise.")
  
  (-find-by-email [this email]
    "Returns entity if it's found, returns nil otherwise."))

#_"I don't like the plural form, but otherwise postgres requires quotes."
#_"\"user\""
(def ^:private tname :users)

(def ^:private sanitize
  (m/decoder schemas/user-out mt/strip-extra-keys-transformer))

(defrecord UserTable [db]
  UserTableOperations
  (-create [this]
    (create-table db tname ["id            int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                            "uid           uuid NOT NULL UNIQUE"
                            "name          text NOT NULL"
                            "email         text NOT NULL UNIQUE"
                            "password_hash text NOT NULL"
                            "role          text NOT NULL"]))
  (-populate [this]
    (populate-table db tname []))
  (-add [this entity]
    (add-entity db tname entity sanitize))
  (-get [this id]
    (get-entity db tname id sanitize))
  (-get-all [this]
    (get-all-entities db tname sanitize))
  (-update [this id entity]
    (update-entity db tname id entity sanitize))
  (-delete [this id]
    (delete-entity db tname id sanitize))

  (-find-by-email [this email]
    (-> (sql/find-by-keys db tname {:email email} jdbc-opts)
        (first))))
