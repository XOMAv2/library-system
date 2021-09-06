(ns service.library.tables.order
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :as udb]
            [service.library.tables.library :as library]))

(defprotocol OrderTableOperations
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
    "Returns deleted entity if it's found, returns nil otherwise."))

#_"I don't like the plural form, but otherwise postgres requires quotes."
#_"\"order\""
(def ^:private tname :orders)

(def ^:private sanitize
  (m/decoder schemas/order-out mt/strip-extra-keys-transformer))

(defrecord OrderTable [db]
  OrderTableOperations
  (-create [this]
    (let [colls ["id             int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                 "uid            uuid NOT NULL UNIQUE"
                 "library_uid    uuid"
                 "book_uid       uuid"
                 "user_uid       uuid"
                 "booking_date   timestamp NOT NULL"
                 "receiving_date timestamp CHECK ((receiving_date IS NULL) OR (receiving_date >= booking_date))"
                 "return_date    timestamp CHECK ((return_date IS NULL) OR (receiving_date IS NOT NULL AND
                                                                            return_date >= receiving_date AND
                                                                            condition IS NOT NULL))"
                 "condition      text" #_"Book condition after returning."
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
    (udb/add-entity db tname entity sanitize))
  (-get [this id]
    (udb/get-entity db tname id sanitize))
  (-get-all [this]
    (udb/get-all-entities db tname sanitize))
  (-update [this id entity]
    (udb/update-entity db tname id entity sanitize))
  (-delete [this id]
    (udb/delete-entity db tname id sanitize)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def config
    (load-config "config.edn" {:profile :local}))

  (def db-config
    (get-in config [:service.library.system/db :db-config]))

  (def db
    (jdbc/get-datasource db-config))

  (def order-table
    (->OrderTable db))

  (-create order-table)

  (-add order-table {:library_uid #uuid "9e916041-daa0-448d-afb6-f1286df90393"
                     :book_uid (java.util.UUID/randomUUID)
                     :user_uid (java.util.UUID/randomUUID)
                     :booking_date #inst "2021-09-03"
                     :receiving_date #inst "2021-09-04"
                     :return_date #inst "2021-09-05"
                     :condition nil})

  (-get order-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")
  (-delete order-table #uuid "8012998b-30a8-486d-af05-35274bda2282")
  (-get-all order-table)
  (jdbc/execute-one! db ["DROP TABLE orders"])
  )
