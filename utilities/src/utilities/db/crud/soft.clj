(ns utilities.db.crud.soft
  (:require [utilities.db.core :refer [jdbc-opts map->honey-sql-exprs]]
            [utilities.core :refer [map-vals]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time]
            [honey.sql :as h]
            [next.jdbc.result-set :refer [ReadableColumn]])
  (:import [java.sql Array]))

(extend-protocol ReadableColumn
  Array
  (read-column-by-label [^Array v _] (vec (.getArray v)))
  (read-column-by-index [^Array v _ _] (vec (.getArray v))))

(defn add-entity
  "Returns entity if it's added.
   Throws exception if entity is malformed."
  ([db tname entity]
   (add-entity db tname entity identity))
  ([db tname entity sanitize]
   (let [entity (map-vals #(if (sequential? %) [:text-array %] %) entity)
         entity (merge entity
                       {:uid (java.util.UUID/randomUUID)
                        :is-deleted false})
         query (h/format {:insert-into [tname]
                          :values [entity]})]
     (-> (jdbc/execute-one! db query jdbc-opts)
         (sanitize)))))

(defn get-entity
  "Returns entity if it's found, returns nil otherwise."
  ([db tname id]
   (get-entity db tname id identity))
  ([db tname id sanitize]
   (let [pk-name (if (uuid? id) :uid :id)
         query (h/format {:select [:*]
                          :from [tname]
                          :where [:and
                                  [:= :is-deleted false]
                                  [:= pk-name id]]})]
     (-> (jdbc/execute-one! db query jdbc-opts)
         (sanitize)))))

(defn get-entity-by-keys
  "Returns first of result entities with matching column values according to keys map."
  ([db tname keys]
   (get-entity-by-keys db tname keys identity))
  ([db tname keys sanitize]
   (let [exprs (if-let [es (-> keys map->honey-sql-exprs seq)]
                 (conj (vec es) [:= :is-deleted false])
                 [:= :is-deleted false])
         query (h/format {:select [:*]
                          :from [tname]
                          :where exprs})]
     (-> (jdbc/execute-one! db query jdbc-opts)
         (sanitize)))))

(defn get-all-entities
  "Returns collection of entities if table isn't empty, returns empty collection otherwise."
  ([db tname]
   (get-all-entities db tname identity))
  ([db tname sanitize]
   (let [query (h/format {:select [:*]
                          :from [tname]
                          :where [:= :is-deleted false]})]
     (->> (jdbc/execute! db query jdbc-opts)
          (map sanitize)))))

(defn get-all-entities-by-keys
  "Returns all of result entities with matching column values according to keys map."
  ([db tname keys]
   (get-all-entities-by-keys db tname keys identity))
  ([db tname keys sanitize]
   (let [exprs (if-let [es (-> keys map->honey-sql-exprs seq)]
                 (conj (vec es) [:= :is-deleted false])
                 [:= :is-deleted false])
         query (h/format {:select [:*]
                          :from [tname]
                          :where exprs})]
     (->> (jdbc/execute! db query jdbc-opts)
          (map sanitize)))))

(defn update-entity
  "Returns updated entity if it's found, returns nil otherwise.
   Throws exception if entity is malformed."
  ([db tname id entity]
   (update-entity db tname id entity identity))
  ([db tname id entity sanitize]
   (let [entity (map-vals #(if (sequential? %) [:text-array %] %) entity)
         pk-name (if (uuid? id) :uid :id)
         query (h/format {:update tname
                          :set entity
                          :where [:and
                                  [:= :is-deleted false]
                                  [:= pk-name id]]})]
     (-> (jdbc/execute-one! db query jdbc-opts)
         (sanitize)))))

(defn update-all-entities-by-keys
  ""
  ([db tname keys entity]
   (update-all-entities-by-keys db tname keys entity identity))
  ([db tname keys entity sanitize]
   (let [exprs (if-let [es (-> keys map->honey-sql-exprs seq)]
                 (conj (vec es) [:= :is-deleted false])
                 [:= :is-deleted false])
         entity (map-vals #(if (sequential? %) [:text-array %] %) entity)
         query (h/format {:update tname
                          :set entity
                          :where exprs})]
     (->> (jdbc/execute! db query jdbc-opts)
          (map sanitize)))))

(defn delete-entity
  "Returns deleted entity if it's found, returns nil otherwise."
  ([db tname id]
   (delete-entity db tname id identity))
  ([db tname id sanitize]
   (let [pk-name (if (uuid? id) :uid :id)
         query (h/format {:update tname
                          :set {:is-deleted true}
                          :where [:and
                                  [:= :is-deleted false]
                                  [:= pk-name id]]})]
     (-> (jdbc/execute-one! db query jdbc-opts)
         (sanitize)))))

(defn delete-all-entities
  "Returns collection of deleted entities if table isn't empty,
   returns empty collection otherwise."
  ([db tname]
   (delete-all-entities db tname identity))
  ([db tname sanitize]
   (let [query (h/format {:update tname
                          :set {:is-deleted true}
                          :where [:= :is-deleted false]})]
     (->> (jdbc/execute! db query jdbc-opts)
          (map sanitize)))))

(defn delete-all-entities-by-keys
  ""
  ([db tname keys]
   (delete-all-entities-by-keys db tname keys identity))
  ([db tname keys sanitize]
   (let [exprs (if-let [es (-> keys map->honey-sql-exprs seq)]
                 (conj (vec es) [:= :is-deleted false])
                 [:= :is-deleted false])
         query (h/format {:update tname
                          :set {:is-deleted true}
                          :where exprs})]
     (->> (jdbc/execute! db query jdbc-opts)
          (map sanitize)))))

(defn restore-entity
  ""
  ([db tname id]
   (restore-entity db tname id identity))
  ([db tname id sanitize]
   (let [pk-name (if (uuid? id) :uid :id)
         query (h/format {:update tname
                          :set {:is-deleted false}
                          :where [:and
                                  [:= :is-deleted true]
                                  [:= pk-name id]]})]
     (-> (jdbc/execute-one! db query jdbc-opts)
         (sanitize)))))

(defn restore-all-entities-by-keys
  ""
  ([db tname keys]
   (restore-all-entities-by-keys db tname keys identity))
  ([db tname keys sanitize]
   (let [exprs (if-let [es (-> keys map->honey-sql-exprs seq)]
                 (conj (vec es) [:= :is-deleted true])
                 [:= :is-deleted true])
         query (h/format {:update tname
                          :set {:is-deleted false}
                          :where exprs})]
     (->> (jdbc/execute! db query jdbc-opts)
          (map sanitize)))))
