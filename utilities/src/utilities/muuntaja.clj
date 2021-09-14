(ns utilities.muuntaja
  (:require [muuntaja.core :as muuntaja]
            [malli.impl.util :refer [map->SchemaError]]
            [camel-snake-kebab.core :as csk]))

(def muuntaja-instance
  (-> muuntaja/default-options
      (muuntaja/select-formats ["application/json"
                                "application/edn"])
      (assoc-in [:formats "application/json" :opts]
                {:encode-key-fn csk/->camelCaseSymbol
                 :decode-key-fn csk/->kebab-case-keyword})
      (assoc-in
       [:formats "application/edn" :decoder-opts]
       {:readers {'Error map->SchemaError}})
      (muuntaja/create)))
