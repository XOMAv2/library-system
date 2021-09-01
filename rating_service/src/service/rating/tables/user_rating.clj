(ns service.rating.tables.user-rating)

(ns service.rating.tables.user-rating
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db :as udb]
            [clojure.math.numeric-tower :refer [abs]]))

(defprotocol UserRatingTableOperations
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
  (-update-rating-by-user-uid [this user-uid delta]
    "Returns updated entity if it's found, returns nil otherwise.
     Throws exception if entity is malformed.")
  (-delete [this id]
    "Returns deleted entity if it's found, returns nil otherwise."))

(def ^:private tname :user_rating)

(def ^:private sanitize
  (m/decoder schemas/user-rating-out mt/strip-extra-keys-transformer))

(defrecord UserRatingTable [db]
  UserRatingTableOperations
  (-create [this]
    (udb/create-table db tname ["id       int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                                "uid      uuid NOT NULL UNIQUE"
                                "user_uid uuid NOT NULL UNIQUE"
                                "rating   int NOT NULL CHECK (rating >= 0)"]))
  (-populate [this]
    (udb/populate-table db tname []))
  (-add [this entity]
    (udb/add-entity db tname entity sanitize))
  (-get [this id]
    (udb/get-entity db tname id sanitize))
  (-get-by-user-uid [this user-uid]
    (udb/get-entity-by-keys db tname {:user_uid user-uid} sanitize))
  (-get-all [this]
    (udb/get-all-entities db tname sanitize))
  (-update [this id entity]
    (udb/update-entity db tname id entity sanitize))
  (-update-rating-by-user-uid [this user-uid delta]
    (let [sign (if (neg? delta) "-" "+")
          query (format "UPDATE %s
                         SET rating = rating %s %d
                         WHERE user_uid = '%s'"
                        (name tname) sign (abs delta) user-uid)]
      (-> (jdbc/execute-one! db [query] udb/jdbc-opts)
          (sanitize))))
  (-delete [this id]
    (udb/delete-entity db tname id sanitize)))

(comment
  (require '[utilities.config :refer [load-config]])

  (def config
    (load-config "config.edn" {:profile :local}))

  (def db-config
    (get-in config [:service.rating.system/db :db-config]))

  (def db
    (jdbc/get-datasource db-config))

  (def user-rating-table
    (->UserRatingTable db))

  (-create user-rating-table)

  (-get-by-user-uid user-rating-table #uuid "fcffadfb-a200-4165-9ca9-0010530ee970")

  (-get-all user-rating-table)

  (-add user-rating-table {:user_uid (java.util.UUID/randomUUID)
                           :rating 54})
  )
