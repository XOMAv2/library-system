(ns service.frontend.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::db
  (fn [db _]
    db))

(rf/reg-sub ::current-view
  (fn [db _]
    (-> db :ui-state :view)))

(rf/reg-sub ::current-modal
  (fn [db _]
    (-> db :ui-state :modal)))

(rf/reg-sub ::current-match
  (fn [db _]
    (:route-match db)))

(rf/reg-sub ::current-route-name
  :<- [::current-match]
  (fn [current-match _]
    (-> current-match :data :name)))

(rf/reg-sub ::user-uid
  (fn [db _]
    (:user-uid db)))

(rf/reg-sub ::users
  (fn [db _]
    (-> db :entities :users)))

(rf/reg-sub ::user-limits
  (fn [db _]
    (-> db :entities :user-limits)))

(rf/reg-sub ::user-ratings
  (fn [db _]
    (-> db :entities :user-ratings)))

(rf/reg-sub ::user
  :<- [::user-uid]
  :<- [::users]
  (fn [[user-uid users] _]
    (-> users user-uid)))

(rf/reg-sub ::libraries
  (fn [db _]
    (-> db :entities :libraries)))

(rf/reg-sub ::books
  (fn [db _]
    (-> db :entities :books)))

(rf/reg-sub ::library-books
  (fn [db [_ {:keys [library-uid book-uid]}]]
    (let [pred (cond
                 (and (some? library-uid) (some? book-uid))
                 (fn [[k v]] (and (= library-uid (:library-uid v))
                              (= book-uid (:book-uid v))))

                 (some? library-uid)
                 (fn [[k v]] (= library-uid (:library-uid v)))

                 (some? book-uid)
                 (fn [[k v]] (= book-uid (:book-uid v)))

                 :else
                 (constantly true))
          f (fn [kv] (into {} (filter pred kv)))]
      (-> db :entities :library-books f))))

(rf/reg-sub ::orders
  (fn [db _]
    (-> db :entities :orders)))

(rf/reg-sub ::stat-records
  (fn [db _]
    (-> db :entities :stat-records)))

(rf/reg-sub ::modal?
  :<- [::current-modal]
  (fn [current-modal _]
    (some? current-modal)))

(rf/reg-sub ::user-role
  (fn [db _]
    (:user-role db)))

(rf/reg-sub ::admin?
  :<- [::user-role]
  (fn [user-role _]
    (= "admin" user-role)))
