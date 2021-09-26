(ns utilities.schemas
  (:require [clojure.string]
            [malli.core :as m]
            [malli.transform :as mt]
            [malli.util :as mu]
            #?(:clj [utilities.time :as time])
            [#?(:clj clojure.core.match :cljs cljs.core.match) :refer [match]]))

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

(def user-out-extended
  (mu/merge user-out
            [:map
             [:total-limit {:optional true} nat-int?]
             [:available-limit {:optional true} int?]
             [:rating {:optional true} nat-int?]]))

(def token-pair
  [:map
   [:access-token non-empty-string]
   [:refresh-token non-empty-string]])

(def client-out
  [:map
   [:uid uuid?]
   [:client-id non-empty-string]
   [:client-secret non-empty-string]
   [:role role]])

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
    [:available-limit int?]]
   [:fn (fn [{:keys [total-limit available-limit]}]
          (<= available-limit total-limit))]])

(def user-limit-update
  [:map
   [:user-uid uuid?]]
  #_[:and
     [:map
      [:user-uid {:optional true} uuid?]
      [:total-limit {:optional true} nat-int?]
      [:available-limit {:optional true} int?]]
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
    [:available-limit int?]]
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
  (let [->vector #(cond (and (string? %) (clojure.string/blank? %)) []
                        (string? %) [%]
                        :else %)]
    (-> book-add
        (mu/optional-keys)
        (mu/update :authors mu/update-properties assoc :decode/string ->vector)
        (mu/update :genres mu/update-properties assoc :decode/string ->vector))))

(def library-add
  [:map
   [:name non-empty-string]
   [:address non-empty-string]
   [:schedule [:sequential non-empty-string]]])

(def library-update
  (mu/optional-keys library-add))

(def library-out
  (mu/assoc library-add :uid uuid?))

(def library-query
  [:map
   [:name {:optional true} non-empty-string]
   [:address {:optional true} non-empty-string]])

(def condition
  [:and
   [string? {:decode/string clojure.string/lower-case}]
   [:enum "normal" "poor" "terrible"]])

(def order-add
  [:and
   [:map
    [:library-uid uuid?]
    [:book-uid uuid?]
    [:user-uid uuid?]
    [:booking-date inst?]
    [:receiving-date {:optional true} inst?]]
   [:fn (fn [{:keys [booking-date receiving-date]}]
          (if (some? receiving-date)
            (#?(:clj time/<= :cljs <=) booking-date receiving-date)
            true))]])

(def order-update
  [:and
   (mu/optional-keys
    [:map
     [:library-uid uuid?]
     [:book-uid [:maybe uuid?]]
     [:user-uid [:maybe uuid?]]
     [:booking-date inst?]
     [:receiving-date inst?]
     [:return-date inst?]
     [:condition condition]])
   [:fn (fn [{:keys [booking-date receiving-date return-date]}]
          (let [loe #?(:clj time/<= :cljs <=)]
            (match (mapv some? [booking-date receiving-date return-date])
              [false true  true]  (loe receiving-date return-date)
              [true  false true]  (loe booking-date return-date)
              [true  true  false] (loe booking-date receiving-date)
              [true  true  true]  (loe booking-date receiving-date return-date)
              :else true)))]])

(def order-out
  [:and
   [:map
    [:uid uuid?]
    [:library-uid [:maybe uuid?]]
    [:book-uid [:maybe uuid?]]
    [:user-uid [:maybe uuid?]]
    [:booking-date inst?]
    [:receiving-date [:maybe inst?]]
    [:return-date [:maybe inst?]]
    [:condition [:maybe condition]]]
   [:fn (fn [{:keys [receiving-date return-date condition]}]
          (let [loe #?(:clj time/<= :cljs <=)]
            (match (mapv some? [receiving-date return-date condition])
              [false false false] true
              [true  false false] true
              [true  true  true]  (loe receiving-date return-date)
              :else false)))]])

(def order-out-extended
  [:and
   [:map
    [:uid uuid?]
    [:library-uid [:maybe uuid?]]
    [:library (mu/update-properties library-out assoc :optional true)]
    [:book-uid [:maybe uuid?]]
    [:book (mu/update-properties book-out assoc :optional true)]
    [:user-uid [:maybe uuid?]]
    [:user (mu/update-properties user-out assoc :optional true)]
    [:booking-date inst?]
    [:receiving-date [:maybe inst?]]
    [:return-date [:maybe inst?]]
    [:condition [:maybe condition]]]
   [:fn (fn [{:keys [receiving-date return-date condition]}]
          (let [loe #?(:clj time/<= :cljs <=)]
            (match (mapv some? [receiving-date return-date condition])
              [false false false] true
              [true  false false] true
              [true  true  true]  (loe receiving-date return-date)
              :else false)))]])

(def order-query
  (-> order-update
      (m/children)
      (first)
      (mu/optional-keys)))

(def library-book-add
  [:and
   [:map
    [:library-uid uuid?]
    [:book-uid uuid?]
    [:total-quantity nat-int?]
    [:granted-quantity nat-int?]
    [:is-available boolean?]]
   [:fn (fn [{:keys [total-quantity granted-quantity]}]
          (<= granted-quantity total-quantity))]])

(def library-book-update
  [:and
   (mu/optional-keys
    [:map
     [:library-uid uuid?]
     [:book-uid uuid?]
     [:total-quantity nat-int?]
     [:granted-quantity nat-int?]
     [:is-available boolean?]])
   [:fn (fn [{:keys [total-quantity granted-quantity]}]
          (if (and total-quantity granted-quantity)
            (<= granted-quantity total-quantity)
            true))]])

(def library-book-out
  [:and
   [:map
    [:uid uuid?]
    [:library-uid uuid?]
    [:book-uid uuid?]
    [:total-quantity nat-int?]
    [:granted-quantity nat-int?]
    [:is-available boolean?]]
   [:fn (fn [{:keys [total-quantity granted-quantity]}]
          (<= granted-quantity total-quantity))]])

(def library-book-query
  [:map
   [:library-uid {:optional true} uuid?]
   [:book-uid {:optional true} uuid?]])

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
