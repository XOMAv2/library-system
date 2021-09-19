(ns utilities.db.core
  (:require [next.jdbc :as jdbc]
            [honey.sql :as h]
            [clojure.string]))

(def jdbc-opts
  (merge jdbc/unqualified-snake-kebab-opts
         {:return-keys true}))
         
(defn sequential->sql-array [sequential]
  (->> sequential
       (map #(try (name %) (catch Exception _ %)))
       (map #(if (string? %) (str "\"" % "\"") (format "%s" %)))
       (clojure.string/join ", ")
       (format "'{%s}'")))

(h/register-fn! :contains (fn [op [a b]]
                            (let [[sql-a & params-a] (h/format-expr a)
                                  [sql-b & params-b] (h/format-expr b)]
                              (-> [(str sql-a " @> " sql-b)]
                                  (into params-a)
                                  (into params-b)))))

(h/register-fn! :text-array (fn [op [sequential]]
                              (let [sequential (map #(vector :inline %) sequential)
                                    sequential [:array sequential]
                                    [sql] (h/format-expr sequential)
                                    sql (str sql "::text[]")]
                                [sql])))

(defn map->honey-sql-exprs [m]
  (if (empty? m)
    []
    (->> m
         (map (fn [[k v :as kv]]
                (if (sequential? v)
                  [:contains k [:text-array v]]
                  (into [:=] kv))))
         (into [:and]))))

(defn create-table
  "Returns nil if table is created or already exists, throws exception otherwise."
  [db tname colls]
  (let [colls (clojure.string/join ", " colls)
        query (str "CREATE TABLE IF NOT EXISTS " (name tname) " ( " colls " );")]
    (jdbc/execute-one! db [query] jdbc-opts)))

(defn populate-table
  "Populates empty table with default data and returns number of added rows.
   Returns nil if table isn't empty."
  [db tname defaults & {:keys [delete-mode] :or {delete-mode :hard}}]
  (let [query (h/format {:select [:*]
                         :from [tname]})
        entities (jdbc/execute! db query jdbc-opts)]
    (when-not (seq entities)
      (->> (for [entity defaults
                 :let [entity (merge entity
                                     {:uid (java.util.UUID/randomUUID)}
                                     (when (= delete-mode :soft)
                                       {:is-deleted false}))]]
             (try (let [query (h/format {:insert-into [tname]
                                         :values [entity]})]
                    (jdbc/execute-one! db query jdbc-opts)
                    1)
                  (catch Exception _ 0)))
           (reduce +)))))
