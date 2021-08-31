(ns utilities.config
  "In case of ClojureScript Node.js is required."
  (:require [integrant.core :as ig]
            [aero.core :as aero]))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn load-config
  "Loading a configuration file from an edn-file with support for the #ig/rek tag
   and tags defined in the aero library."
  [filename opts]
  (aero/read-config filename opts))
