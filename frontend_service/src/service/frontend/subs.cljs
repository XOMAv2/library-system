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

(rf/reg-sub ::modal?
  :<- [::current-modal]
  (fn [current-modal _]
    (some? current-modal)))
