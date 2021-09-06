(ns utilities.schemas
  (:require [clojure.string]
            [malli.core :as m]
            [malli.transform :as mt]
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
   [:body non-empty-string]
   [:content-type {:optional true} non-empty-string]
   [:send-time inst?]])

(def stat-record-update
  (-> stat-record-add
      (mu/assoc :receive-time inst?)
      (mu/optional-keys)))

(def stat-record-out
  [:map
   [:uid uuid?]
   [:service non-empty-string]
   [:body non-empty-string]
   [:content-type non-empty-string]
   [:send-time inst?]
   [:receive-time inst?]])

(def user-limit-add
  [:and
   [:map
    [:user-uid uuid?]
    [:total-limit nat-int?]
    [:available-limit nat-int?]]
   [:fn (fn [{:keys [total-limit available-limit]}]
          (<= available-limit total-limit))]])

(def user-limit-update
  [:and
   [:map
    [:user-uid {:optional true} uuid?]
    [:total-limit {:optional true} nat-int?]
    [:available-limit {:optional true} nat-int?]]
   [:fn (fn [{:keys [total-limit available-limit]}]
          (if (and total-limit available-limit)
            (<= available-limit total-limit)
            true))]])

(def user-limit-out
  [:and
   [:map
    [:uid uuid?]
    [:user-uid uuid?]
    [:total-limit nat-int?]
    [:available-limit nat-int?]]
   [:fn (fn [{:keys [total-limit available-limit]}]
          (<= available-limit total-limit))]])

(def user-rating-add
  [:map
   [:user-uid uuid?]
   [:rating nat-int?]])

(def user-rating-update
  (mu/optional-keys user-rating-add))

(def user-rating-out
  (mu/assoc user-rating-add :uid uuid?))

(def book-add
  [:map
   [:name non-empty-string]
   [:authors [:sequential non-empty-string]]
   [:genres [:sequential non-empty-string]]
   [:description non-empty-string]
   [:price nat-int?]])

(def book-update
  (mu/optional-keys book-add))

(def book-out
  (mu/assoc book-add :uid uuid?))

(def book-query
  (let [->vector #(if (vector? %) % [%])]
    (-> book-add
        (mu/optional-keys)
        (mu/update :authors mu/update-properties assoc :decode/string ->vector)
        (mu/update :genres mu/update-properties assoc :decode/string ->vector))))

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
