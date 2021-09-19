(ns utilities.db.tables.client
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.db.core :as udb]
            [utilities.db.crud.hard :as crud]
            [buddy.hashers :as hashers]))

(defprotocol ClientTableOperations
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
  (-get-by-client-id [this client-id]
    "Returns entity if it's found, returns nil otherwise.")
  (-get-all [this]
    "Returns collection of entities if table isn't empty, returns empty collection otherwise.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise."))

(def ^:private tname :client)

(defonce clients
  (->> [{:client-id "book" :client-secret "book" :role "admin" :is-deleted false}
        {:client-id "gateway" :client-secret "gateway" :role "admin" :is-deleted false}
        {:client-id "library" :client-secret "library" :role "admin" :is-deleted false}
        {:client-id "rating" :client-secret "rating" :role "admin" :is-deleted false}
        {:client-id "return" :client-secret "return" :role "admin" :is-deleted false}
        {:client-id "session" :client-secret "session" :role "admin" :is-deleted false}
        {:client-id "stats" :client-secret "stats" :role "admin" :is-deleted false}]
       (mapv #(update % :client-secret hashers/derive {:alg :bcrypt+sha512}))))

(defrecord ClientTable [db]
  ClientTableOperations
  (-create [this]
    (udb/create-table db tname ["id            int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                                "uid           uuid NOT NULL UNIQUE"
                                "client_id     text NOT NULL UNIQUE"
                                "client_secret text NOT NULL"
                                "role          text NOT NULL"]))
  (-populate [this]
    (udb/populate-table db tname clients))
  (-add [this entity]
    (crud/add-entity db tname entity))
  (-get [this id]
    (crud/get-entity db tname id))
  (-get-by-client-id [this client-id]
    (crud/get-entity-by-keys db tname {:client_id client-id}))
  (-get-all [this]
    (crud/get-all-entities db tname))
  (-update [this id entity]
    (crud/update-entity db tname id entity))
  (-delete [this id]
    (crud/delete-entity db tname id)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def db-config
    (-> (load-config "../rating_service/config.edn" {:profile :local})
        (get-in [:service.rating.system/db :db-config])))

  (def db
    (jdbc/get-datasource db-config))

  (def client-table (->ClientTable db))

  (-get-all client-table)

  (-get client-table #uuid "0496300c-2d37-48a2-9f06-6d69af7727fe")

  )
