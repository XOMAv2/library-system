(ns utilities.core
  #?(:cljs (:require-macros [utilities.core :refer [when-let*]]))
  (:require [clojure.string]))

(defn remove-trailing-slash [uri]
  (if (= \/ (last uri))
    (apply str (butlast uri))
    uri))

(defn non-empty-string? [x]
  (and (string? x)
       (not (clojure.string/blank? x))))

#?(:clj (defn safe-println [& more]
          (.write *out* (str (clojure.string/join " " more) "\n"))))

#?(:clj (defmacro when-let*
          [bindings & body]
          (if (seq bindings)
            `(when-let [~(first bindings) ~(second bindings)]
               (when-let* ~(drop 2 bindings) ~@body))
            `(do ~@body))))

(defn update-vals [m f & more]
  (->> m
       (map (fn [[k v]]
              [k (apply f v more)]))
       (into {})))

#?(:clj (defn parse-content-type
          "Exctract MIME type from Content-Type header value."
          [^String s]
          (let [i (.indexOf s ";")]
            (if (neg? i) s (.substring s 0 i))))
   :cljs (defn parse-content-type
           "Exctract MIME type from Content-Type header value."
           [s]
           (first (clojure.string/split s #";"))))

(defn- editable? [coll]
  #?(:clj (instance? clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection coll)))

(defn- reduce-map [f coll]
  (let [coll' (if (record? coll) (into {} coll) coll)]
    (if (editable? coll')
      (persistent! (reduce-kv (f assoc!) (transient (empty coll')) coll'))
      (reduce-kv (f assoc) (empty coll') coll'))))

#_"https://github.com/weavejester/medley"
(defn map-keys
  "Maps a function over the keys of an associative collection."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m (f k) v))) coll))

(defn map-vals
  "Maps a function over the values of one or more associative collections.
  The function should accept number-of-colls arguments. Any keys which are not
  shared among all collections are ignored."
  ([f coll]
   (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))
  ([f c1 & colls]
   (reduce-map
    (fn [xf]
      (fn [m k v]
        (if (every? #(contains? % k) colls)
          (xf m k (apply f v (map #(get % k) colls)))
          m)))
    c1)))

(defn any-or-coll->coll [any-or-coll]
  (if (coll? any-or-coll)
    any-or-coll
    [any-or-coll]))

(defn class-concat
  "`class` - either a string or a collection of strings in terms of hiccup html classes.
   Returns collection of strings."
  [& classes]
  (->> classes
       (filter some?)
       (map any-or-coll->coll)
       (apply concat)))

(defn remove-nth
  "Returns a lazy sequence of the items in coll, except for the item at the
  supplied index. Runs in O(n) time. Returns a transducer when no collection is
  provided."
  {:added "1.2.0"}
  ([index]
   (fn [rf]
     (let [idx (volatile! (inc index))]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result x]
          (if (zero? (vswap! idx dec))
            result
            (rf result x)))))))
  ([index coll]
   (lazy-seq
    (if (zero? index)
      (rest coll)
      (when (seq coll)
        (cons (first coll) (remove-nth (dec index) (rest coll))))))))

(defn dissoc-in
  "Dissociate a value in a nested associative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures."
  ([m ks]
   (if-let [[k & ks] (seq ks)]
     (if (seq ks)
       (let [v (dissoc-in (get m k) ks)]
         (if (empty? v)
           (dissoc m k)
           (assoc m k v)))
       (dissoc m k))
     m))
  ([m ks & kss]
   (if-let [[ks' & kss] (seq kss)]
     (recur (dissoc-in m ks) ks' kss)
     (dissoc-in m ks))))
