(ns service.library.tables.library-book
  (:require [clojure.string]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db.core :as udb]
            [utilities.db.crud.hard :as crud]
            [service.library.tables.library :as library]))

(defprotocol LibraryBookTableOperations
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
  (-get-all-by-keys [this library-book]
    "Returns all of result entities with matching column values according to entity map.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise."))

(def ^:private tname :library_books)

(def ^:private sanitize
  (m/decoder schemas/library-book-out mt/strip-extra-keys-transformer))

(defrecord LibraryBookTable [db]
  LibraryBookTableOperations
  (-create [this]
    (let [colls ["id               int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                 "uid              uuid NOT NULL UNIQUE"
                 "library_uid      uuid"
                 "book_uid         uuid"
                 #_"Количество экземпляров конкретной книги на балансе конкретной библиотеки."
                 "total_quantity   int NOT NULL CHECK (total_quantity >= 0)"
                 #_"Количество бронированных и/или выданных экземпляров конкретной книги в конкретной библиотеке."
                 "granted_quantity int NOT NULL CHECK (granted_quantity >= 0 AND granted_quantity <= total_quantity)"
                 "is_available     boolean NOT NULL"
                 (str "FOREIGN KEY (library_uid) "
                      "REFERENCES " (-> #'library/tname var-get name) " (uid) "
                      "ON DELETE SET NULL")]
          colls (clojure.string/join ", " colls)
          query (str "CREATE TABLE IF NOT EXISTS " (name tname) " ( " colls " );")]
      (jdbc/execute-one! db [query])
      nil))
  (-populate [this]
    (udb/populate-table db tname []))
  (-add [this entity]
    (crud/add-entity db tname entity sanitize))
  (-get [this id]
    (crud/get-entity db tname id sanitize))
  (-get-all [this]
    (crud/get-all-entities db tname sanitize))
  (-get-all-by-keys [this library-book]
    (crud/get-all-entities-by-keys db tname library-book sanitize))
  (-update [this id entity]
    (crud/update-entity db tname id entity sanitize))
  (-delete [this id]
    (crud/delete-entity db tname id sanitize)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def config
    (load-config "config.edn" {:profile :local}))

  (def db-config
    (get-in config [:service.library.system/db :db-config]))

  (def db
    (jdbc/get-datasource db-config))

  (def library-book-table
    (->LibraryBookTable db))

  (-create library-book-table)

  (-add library-book-table {:library_uid #uuid "9e916041-daa0-448d-afb6-f1286df90393"
                             :book-uid (java.util.UUID/randomUUID)
                             :total-quantity 100
                             :granted-quantity 1001
                             :is-available true})

  (-get library-book-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")
  (-delete library-book-table #uuid "8012998b-30a8-486d-af05-35274bda2282")
  (-get-all library-book-table)
  (jdbc/execute-one! db ["DROP TABLE library_books"])
  )
