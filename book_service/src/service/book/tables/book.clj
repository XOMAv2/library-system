(ns service.book.tables.book
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db.core :as udb]
            [utilities.db.crud.soft :as crud]))

(defprotocol BookTableOperations
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
  (-get-all-by-keys [this keys]
    "Returns all of result entities with matching column values according to entity map.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise.")
  (-restore [this id]
    ""))

(def ^:private tname :book)

(def ^:private sanitize
  (m/decoder schemas/book-out mt/strip-extra-keys-transformer))

(defrecord BookTable [db]
  BookTableOperations
  (-create [this]
    (udb/create-table
     db tname
     ["id          int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
      "uid         uuid NOT NULL UNIQUE"
      "name        text NOT NULL"
      "authors     text[] NOT NULL CHECK (array_position(authors, null) is null)"
      "genres      text[] NOT NULL CHECK (array_position(genres, null) is null)"
      "description text NOT NULL"
      "price       int NOT NULL CHECK (price >= 0)"
      "is_deleted  boolean NOT NULL"]))
  (-populate [this]
    (udb/populate-table db tname [] :delete-mode :soft))
  (-add [this entity]
    (crud/add-entity db tname entity sanitize))
  (-get [this id]
    (crud/get-entity db tname id sanitize))
  (-get-all [this]
    (crud/get-all-entities db tname sanitize))
  (-get-all-by-keys [this keys]
    (crud/get-all-entities-by-keys db tname keys sanitize))
  (-update [this id entity]
    (crud/update-entity db tname id entity sanitize))
  (-delete [this id]
    (crud/delete-entity db tname id sanitize))
  (-restore [this id]
    (crud/restore-entity db tname id sanitize)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def config
    (load-config "config.edn" {:profile :local}))

  (def db-config
    (get-in config [:service.book.system/db :db-config]))

  (def db
    (jdbc/get-datasource db-config))

  (def book-table
    (->BookTable db))
  
  (jdbc/execute! db [(str "DROP TABLE " (name tname))])

  (-create book-table)

  (-get-all book-table)

  (-add book-table {:name "name"
                    :authors '("author-1" "a-2")
                    :genres ["6" "5" "4"]
                    :description "description"
                    :price 34})

  (-update book-table
           #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496"
           {:genres []})
  
  (-get book-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")
  (-delete book-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")

  (-get-all-by-keys book-table {:authors ["a-2"]})

  )
