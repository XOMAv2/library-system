(ns service.stats.tables.client
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.db :refer [add-entity get-entity get-entity-by-keys get-all-entities
                                  update-entity delete-entity
                                  create-table populate-table
                                  jdbc-opts]]
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
  (->> [{:client-id "book" :client-secret "book" :role "admin"}
        {:client-id "gateway" :client-secret "gateway" :role "admin"}
        {:client-id "library" :client-secret "library" :role "admin"}
        {:client-id "rating" :client-secret "rating" :role "admin"}
        {:client-id "return" :client-secret "return" :role "admin"}
        {:client-id "session" :client-secret "session" :role "admin"}
        {:client-id "stats" :client-secret "stats" :role "admin"}]
       (mapv #(update % :client-secret hashers/derive {:alg :bcrypt+sha512}))))

(defrecord ClientTable [db]
  ClientTableOperations
  (-create [this]
    (create-table db tname ["id            int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                            "uid           uuid NOT NULL UNIQUE"
                            "client_id     text NOT NULL UNIQUE"
                            "client_secret text NOT NULL"
                            "role          text NOT NULL"]))
  (-populate [this]
    (populate-table db tname clients))
  (-add [this entity]
    (add-entity db tname entity))
  (-get [this id]
    (get-entity db tname id))
  (-get-by-client-id [this client-id]
    (get-entity-by-keys db tname {:client_id client-id}))
  (-get-all [this]
    (get-all-entities db tname))
  (-update [this id entity]
    (update-entity db tname id entity))
  (-delete [this id]
    (delete-entity db tname id)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def db-config
    (-> (load-config "config.edn" {:profile :local})
        (get-in [:service.session.system/db :db-config])))

  (def db
    (jdbc/get-datasource db-config))

  (def client-table (->ClientTable db))
  )
