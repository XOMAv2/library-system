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
