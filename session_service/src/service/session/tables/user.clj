(ns service.session.tables.user
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :as udb]))

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
  (-get-by-email [this email]
    "Returns entity if it's found, returns nil otherwise.")
  (-get-all [this]
    "Returns collection of entities if table isn't empty, returns empty collection otherwise.")
  (-update [this id entity]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise."))

#_"I don't like the plural form, but otherwise postgres requires quotes."
#_"\"user\""
(def ^:private tname :users)

(def ^:private sanitize
  (m/decoder schemas/user-out mt/strip-extra-keys-transformer))

(defrecord UserTable [db]
  UserTableOperations
  (-create [this]
    (udb/create-table db tname ["id            int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                                "uid           uuid NOT NULL UNIQUE"
                                "name          text NOT NULL"
                                "email         text NOT NULL UNIQUE"
                                "password_hash text NOT NULL"
                                "role          text NOT NULL"]))
  (-populate [this]
    (udb/populate-table db tname []))
  (-add [this entity]
    (udb/add-entity db tname entity sanitize))
  (-get [this id]
    (udb/get-entity db tname id sanitize))
  (-get-by-email [this email]
    (udb/get-entity-by-keys db tname {:email email} sanitize))
  (-get-all [this]
    (udb/get-all-entities db tname sanitize))
  (-update [this id entity]
    (udb/update-entity db tname id entity sanitize))
  (-delete [this id]
    (udb/delete-entity db tname id sanitize)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def db-config
    (-> (load-config "config.edn" {:profile :local})
        (get-in [:service.session.system/db :db-config])))

  #_(def db
    {:dbtype "postgres"
     :host "localhost"
     :port 4444
     :dbname "session_db"
     :user "postgres"
     :password "postgres"})
  
  (def db
    (jdbc/get-datasource db-config))

  (def user-table (->UserTable db))

  (jdbc/execute-one! db [(str "DROP TABLE " (name tname))])

  (-create user-table)

  (-populate user-table)

  (-delete user-table #uuid "f1a1425c-e5f1-4591-a20f-1ccd4da3714f")

  (-get-all user-table)

  (-update user-table 14 {:name "Nikki"})

  (-get-by-email user-table "lol322")

  (try (-add user-table {:name "nil6"
                         :email "lol2"
                         :password-hash "lell"
                         :role "role"})
       (catch Exception e
         [(type e) (ex-message e)]))
  )
