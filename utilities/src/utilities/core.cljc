(ns utilities.core
  (:require [clojure.string]))

(defn remove-trailing-slash [uri]
  (if (= \/ (last uri))
    (apply str (butlast uri))
    uri))

(defn non-empty-string? [x]
  (and (string? x)
       (not (clojure.string/blank? x))))
