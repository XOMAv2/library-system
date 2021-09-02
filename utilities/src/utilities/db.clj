(ns utilities.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.date-time]))

(def jdbc-opts
  (merge jdbc/unqualified-snake-kebab-opts
         {:return-keys true}))

(defn add-entity
  "Returns entity if it's added.
   Throws exception if entity is malformed."
  ([db tname entity]
   (add-entity db tname entity identity))
  ([db tname entity sanitize]
   (as-> entity $
     (assoc $ :uid (java.util.UUID/randomUUID))
     (sql/insert! db tname $ jdbc-opts)
     (sanitize $))))

(defn get-entity
  "Returns entity if it's found, returns nil otherwise."
  ([db tname id]
   (get-entity db tname id identity))
  ([db tname id sanitize]
   (let [pk-name (if (uuid? id) :uid :id)
         entity (sql/get-by-id db tname id pk-name jdbc-opts)
         entity (sanitize entity)]
     entity)))

(defn get-entity-by-keys
  "Returns first of result entities with matching column values according to keys map."
  ([db tname keys]
   (get-entity-by-keys db tname keys identity))
  ([db tname keys sanitize]
   (-> (sql/find-by-keys db tname keys jdbc-opts)
       (first)
       (sanitize))))

(defn get-all-entities
  "Returns collection of entities if table isn't empty, returns empty collection otherwise."
  ([db tname]
   (get-all-entities db tname identity))
  ([db tname sanitize]
   (let [query (str "SELECT * FROM " (name tname))]
     (->> (jdbc/execute! db [query] jdbc-opts)
          (map sanitize)))))

(defn get-all-entities-by-keys
  "Returns all of result entities with matching column values according to keys map."
  ([db tname keys]
   (get-all-entities-by-keys db tname keys identity))
  ([db tname keys sanitize]
   (->> (sql/find-by-keys db tname keys jdbc-opts)
        (map sanitize))))

(defn update-entity
  "Returns updated entity if it's found, returns nil otherwise.
   Throws exception if entity is malformed."
  ([db tname id entity]
   (update-entity db tname id entity identity))
  ([db tname id entity sanitize]
   (let [where-params (if (uuid? id) {:uid id} {:id id})]
     (as-> entity $
       (sql/update! db tname $ where-params jdbc-opts)
       (sanitize $)
       (when (not= {} $) $)))))

(defn delete-entity
  "Returns deleted entity if it's found, returns nil otherwise."
  ([db tname id]
   (delete-entity db tname id identity))
  ([db tname id sanitize]
   (let [where-params (if (uuid? id) {:uid id} {:id id})
         entity (sql/delete! db tname where-params jdbc-opts)
         entity (sanitize entity)]
     entity)))

(defn delete-all-entities
  "Returns collection of deleted entities if table isn't empty,
   returns empty collection otherwise."
  ([db tname]
   (delete-all-entities db tname identity))
  ([db tname sanitize]
   (let [query (str "DELETE FROM " (name tname))]
     (->> (jdbc/execute! db [query] jdbc-opts)
          (map sanitize)))))

(defn create-table
  "Returns nil if table is created or already exists, throws exception otherwise."
  [db tname colls]
  (let [colls (clojure.string/join ", " colls)
        query (str "CREATE TABLE IF NOT EXISTS " (name tname) " ( " colls " );")]
    (jdbc/execute-one! db [query] jdbc-opts)))

(defn populate-table
  "Populates empty table with default data and returns number of added rows.
   Returns nil if table isn't empty."
  [db tname defaults]
  (when (= 0 (count (get-all-entities db tname)))
    (->> (for [entity defaults]
           (try (add-entity db tname entity)
                1
                (catch Exception _ 0)))
         (reduce +))))
