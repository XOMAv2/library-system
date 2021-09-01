(ns utilities.schemas
  (:require [clojure.string]
            [malli.core :as m]
            [malli.util :as mu]))

(def non-empty-string
  [:and
   string?
   [:fn (fn [x] (not (clojure.string/blank? x)))]])

(def message
  [:map
   [:message string?]])

(def role
  [:and
   [string? {:decode/string clojure.string/lower-case}]
   [:enum "admin" "reader"]])

(def user-add
  [:map
   [:name non-empty-string]
   [:email non-empty-string]
   [:role role]
   [:password non-empty-string]])

(def user-update
  (mu/optional-keys user-add))

(def user-out
  [:map
   [:uid uuid?]
   [:name non-empty-string]
   [:email non-empty-string]
   [:role role]])

(def token-pair
  [:map
   [:access-token non-empty-string]
   [:refresh-token non-empty-string]])

(def stat-record-add
  [:map
   [:service non-empty-string]
   [:operation non-empty-string]
   [:send-time inst?]])

(def stat-record-update
  (-> stat-record-add
      (mu/assoc :receive-time inst?)
      (mu/optional-keys)))

(def stat-record-out
  [:map
   [:uid uuid?]
   [:service non-empty-string]
   [:operation non-empty-string]
   [:send-time inst?]
   [:receive-time inst?]])

(def db-config
  [:map
   [:dbtype [:enum "postgresql" "postgres"]]
   [:dbname non-empty-string]
   [:host non-empty-string]
   [:port nat-int?]
   [:user non-empty-string]
   [:password non-empty-string]
   [:uri {:optional true} non-empty-string]])

(def services-uri
  [:map-of simple-keyword? non-empty-string])

(def server-options
  [:map
   [:port nat-int?]])
