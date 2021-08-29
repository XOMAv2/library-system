(ns utilities.muuntaja
  (:require [muuntaja.core :as muuntaja]
            [camel-snake-kebab.core :as csk]))

(def muuntaja-instance
  (-> muuntaja/default-options
      (muuntaja/select-formats ["application/json"
                                "application/edn"])
      (assoc-in [:formats "application/json" :opts]
                {:encode-key-fn csk/->camelCaseSymbol
                 :decode-key-fn csk/->kebab-case-keyword})
      (muuntaja/create)))
