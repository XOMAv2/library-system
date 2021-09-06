(ns service.book.tables.book
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :as udb]
            [clojure.string]
            [next.jdbc.prepare :refer [SettableParameter]]
            [next.jdbc.result-set :refer [ReadableColumn]]
            [camel-snake-kebab.core :as csk])
  (:import [java.sql Array PreparedStatement]))

(extend-protocol SettableParameter
  clojure.lang.PersistentVector
  (set-parameter [^clojure.lang.PersistentVector v ^PreparedStatement ps ^long i]
    (.setObject ps i (into-array String v))))

(extend-protocol ReadableColumn
  Array
  (read-column-by-label [^Array v _] (vec (.getArray v)))
  (read-column-by-index [^Array v _ _] (vec (.getArray v))))

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
  (-get-all-by-keys [this book]
    "Returns collection of entities for specified service.
     Returns empty collection if service contains no entities or service wasn't found.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise."))

(def ^:private tname :book)

(def ^:private sanitize
  (m/decoder schemas/book-out mt/strip-extra-keys-transformer))

(defn coll->sql-array [coll]
  (->> (if (map? coll) (vals coll) coll)
       (map #(try (name %) (catch Exception _ %)))
       (map #(if (string? %) (str "\"" % "\"") (str %)))
       (clojure.string/join ", ")
       (format "'{%s}'")))

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
      "price       int NOT NULL CHECK (price >= 0)"]))
  (-populate [this]
    (udb/populate-table db tname []))
  (-add [this entity]
    (udb/add-entity db tname entity sanitize))
  (-get [this id]
    (udb/get-entity db tname id sanitize))
  (-get-all [this]
    (udb/get-all-entities db tname sanitize))
  (-get-all-by-keys [this book]
    (let [conditions (when (not-empty book)
                       (->> (for [[key value] book
                                  :let [key (csk/->snake_case_string key)]]
                              (if (vector? value)
                                #_"@> - contains all of '{}'; && - contains some of '{}'"
                                (str key " @> " (coll->sql-array value))
                                (str key " = " value)))
                            (clojure.string/join " AND ")
                            (str " WHERE ")))
          query (str "SELECT * FROM " (name tname) conditions)]
      (->> (jdbc/execute! db [query] udb/jdbc-opts)
           (map sanitize))))
  (-update [this id entity]
    (udb/update-entity db tname id entity sanitize))
  (-delete [this id]
    (udb/delete-entity db tname id sanitize)))

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

  (-create book-table)

  (-add book-table {:name "name"
                    :authors ["author-1" "a-2"]
                    :genres ["6" "5" "4"]
                    :description "description"
                    :price 34})

  (-update book-table
           #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496"
           {:genres []})
  
  (-get book-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")
  (-delete book-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")
  
  (-get-all-by-keys book-table {:name "name"
                                :authors ["author-1" "a-2"]
                                :genres ["6" "5" "4"]
                                :description "description"
                                :price 34})

  (-get-all book-table)
  (-delete-all book-table)
  )
