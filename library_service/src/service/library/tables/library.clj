(ns service.library.tables.library
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db.core :as udb]
            [utilities.db.crud.hard :as crud]
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

(defprotocol LibraryTableOperations
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
  (-get-all-by-keys [this library]
    "Returns all of result entities with matching column values according to entity map.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise."))

(def ^:private tname :library)

(def ^:private sanitize
  (m/decoder schemas/library-out mt/strip-extra-keys-transformer))

(defrecord LibraryTable [db]
  LibraryTableOperations
  (-create [this]
    (udb/create-table
     db tname
     ["id          int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
      "uid         uuid NOT NULL UNIQUE"
      "name        text NOT NULL"
      "address     text NOT NULL"
      "schedule    text[] NOT NULL CHECK (array_position(schedule, null) is null)"]))
  (-populate [this]
    (udb/populate-table db tname []))
  (-add [this entity]
    (crud/add-entity db tname entity sanitize))
  (-get [this id]
    (crud/get-entity db tname id sanitize))
  (-get-all [this]
    (crud/get-all-entities db tname sanitize))
  (-get-all-by-keys [this library]
    (let [conditions (when (not-empty library)
                       (->> (for [[key value] library
                                  :let [key (csk/->snake_case_string key)]]
                              (if (vector? value)
                                #_"@> - contains all of '{}'; && - contains some of '{}'"
                                (str key " @> " (udb/sequential->sql-array value))
                                (str key " = " value)))
                            (clojure.string/join " AND ")
                            (str " WHERE ")))
          query (str "SELECT * FROM " (name tname) conditions)]
      (->> (jdbc/execute! db [query] udb/jdbc-opts)
           (map sanitize)))
    (crud/get-all-entities-by-keys db tname library sanitize))
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

  (def library-table
    (->LibraryTable db))

  (-create library-table)

  (-add library-table {:name "name"
                       :address "author-1"
                       :schedule ["6" "5" "4"]})
  
  (-update library-table
           #uuid "8012998b-30a8-486d-af05-35274bda2282"
           {:address "lol"
            :schedule []})

  (-get library-table #uuid "68cb327d-cf1f-4c57-8872-e4e8b2f20496")
  (-delete library-table #uuid "971a8bf5-4366-4927-8bb9-51bdf7480427")
  (-get-all library-table)
  (jdbc/execute-one! db ["DROP TABLE library"])
  )
