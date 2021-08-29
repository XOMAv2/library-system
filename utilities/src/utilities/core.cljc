(ns utilities.core
  (:require [integrant.core :as ig]
            [aero.core :as aero]
            [clojure.string]))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn load-config
  "Loading a configuration file from an edn-file with support for the #ig/rek tag
   and tags defined in the aero library."
  [filename opts]
  (aero/read-config filename opts))

(defn remove-trailing-slash [uri]
  (if (= \/ (last uri))
    (apply str (butlast uri))
    uri))

(defn non-empty-string? [x]
  (and (string? x)
       (not (clojure.string/blank? x))))
