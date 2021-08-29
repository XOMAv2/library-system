(ns utilities.conformers
  (:require [clojure.string]
            [utilities.core :refer [non-empty-string?]]
            [#?(:clj clojure.spec.alpha :cljs cljs.spec.alpha) :as s]
            #?(:clj [clojure.instant :refer [read-instant-date]])
            #?(:cljs [cljs.reader :refer [parse-timestamp]])
            #?(:cljs [cljs-uuid-utils.core :refer [make-uuid-from]])))

(s/def ::->date
  (s/conformer
   #(cond (instance? #?(:clj java.util.Date :cljs js/Date) %) %
          (non-empty-string? %) (try (#?(:clj read-instant-date
                                         :cljs parse-timestamp) %)
                                     (catch #?(:clj Exception :cljs js/Error) _
                                       ::s/invalid))
          :else ::s/invalid)))

(s/def ::->int
  (s/conformer
   #(cond (int? %) %
          (string? %) (let [s (clojure.string/replace % #" " "")]
                        #?(:clj (try (Integer/parseInt s)
                                     (catch Exception _ ::s/invalid))
                           :cljs (let [result (js/parseInt s)]
                                   (if (.isNaN js/Number result)
                                     ::s/invalid
                                     result))))
          :else ::s/invalid)))

(s/def ::->uuid
  (s/conformer
   #?(:clj #(cond (uuid? %) %
                  (string? %) (try
                                (java.util.UUID/fromString %)
                                (catch Exception _ ::s/invalid))
                  :else ::s/invalid))
   #_"Current `cljs.core/UUID.` constructor doesn't check for any conformance."
   #?(:cljs #(if-let [uid (make-uuid-from %)]
               uid
               ::s/invalid))))
