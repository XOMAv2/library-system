(ns service.rating.tables.user-rating
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [malli.transform :as mt]
            [utilities.db.core :as udb]
            [utilities.db.crud.soft :as crud]
            [honey.sql :as h]
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
    "Returns deleted entity if it's found, returns nil otherwise.")
  (-delete-by-user-uid [this user-uid]
    "")
  (-restore [this id]
    "")
  (-restore-by-user-uid [this user-uid]
    ""))

(def ^:private tname :user_rating)

(def ^:private sanitize
  (m/decoder schemas/user-rating-out mt/strip-extra-keys-transformer))

(defrecord UserRatingTable [db]
  UserRatingTableOperations
  (-create [this]
    (udb/create-table db tname ["id         int GENERATED ALWAYS AS IDENTITY PRIMARY KEY"
                                "uid        uuid NOT NULL UNIQUE"
                                "user_uid   uuid NOT NULL UNIQUE"
                                "rating     int NOT NULL CHECK (rating >= 0)"
                                "is_deleted boolean NOT NULL"]))
  (-populate [this]
    (udb/populate-table db tname [] :delete-mode :soft))
  (-add [this entity]
    (crud/add-entity db tname entity sanitize))
  (-get [this id]
    (crud/get-entity db tname id sanitize))
  (-get-by-user-uid [this user-uid]
    (crud/get-entity-by-keys db tname {:user_uid user-uid} sanitize))
  (-get-all [this]
    (crud/get-all-entities db tname sanitize))
  (-update [this id entity]
    (crud/update-entity db tname id entity sanitize))
  (-update-rating-by-user-uid [this user-uid delta]
    (let [sign (if (neg? delta) :- :+)
          query (h/format {:update tname
                           :set {:rating [sign :rating (abs delta)]}
                           :where [:and
                                   [:= :is-deleted false]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize))))
  (-delete [this id]
    (crud/delete-entity db tname id sanitize))
  (-delete-by-user-uid [this user-uid]
    (let [query (h/format {:update tname
                           :set {:is-deleted true}
                           :where [:and
                                   [:= :is-deleted false]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize))))
  (-restore [this id]
    (crud/restore-entity db tname id sanitize))
  (-restore-by-user-uid [this user-uid]
    (let [query (h/format {:update tname
                           :set {:is-deleted false}
                           :where [:and
                                   [:= :is-deleted true]
                                   [:= :user-uid user-uid]]})]
      (-> (jdbc/execute-one! db query udb/jdbc-opts)
          (sanitize)))))

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

  (-populate user-rating-table)

  (-get-all user-rating-table)

  (-get-by-user-uid user-rating-table #uuid "fcffadfb-a200-4165-9ca9-0010530ee970")

  (count (-get-all user-rating-table))

  (count (jdbc/execute! db [(str "SELECT * FROM " (name tname))]) )

  (-delete user-rating-table #uuid "8f786062-c788-4cfb-a6e8-c92494308d5c")

  (-add user-rating-table {:user_uid #uuid "8f786062-c788-4cfb-a6e8-c92494308d5c"
                           :rating 54})
  
  (-update user-rating-table #uuid "8f786062-c788-4cfb-a6e8-c92494308d5c"
           {:rating 56})
  
  (-update-rating-by-user-uid user-rating-table #uuid "6a2f0b94-b749-4987-a6ef-a7301bd15aaf"
                              -4)
  
  (-restore user-rating-table #uuid "8f786062-c788-4cfb-a6e8-c92494308d5c")
  )
