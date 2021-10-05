(ns service.frontend.events
  (:require [re-frame.core :as rf]
            [reitit.frontend.controllers :refer [apply-controllers]]
            [service.frontend.effects :as effects]
            [service.frontend.db :as db]
            [service.frontend.router :as routes]
            [service.frontend.views :as views]
            [service.frontend.api.gateway :as gateway]))

(rf/reg-event-fx ::assoc-in-db
  (fn [{:keys [db]} [_ path value]]
    {:db (assoc-in db path value)}))

(rf/reg-event-db ::init-db
  (fn [_ _]
    db/default-db))

(rf/reg-event-fx ::navigate
  (fn [_ [_ params]]
    {::effects/navigate params}))

#_"Это событие не нужно вызывать напрямую из views. Оно вызываться при смене URI."
(rf/reg-event-fx ::on-route-match
  (fn [{:keys [db]} [_ match]]
    (if (some? match)
      {:db (->> (:route-match db)
                (:controllers)
                (#(apply-controllers % match))
                (assoc match :controllers)
                (assoc db :route-match))}
      {:fx [[::effects/navigate {:route ::routes/books}]]})))

(rf/reg-event-fx ::change-view
  (fn [{:keys [db]} [_ view view-scope]]
    {:db (-> db
             (assoc-in [:ui-state :view] view)
             (assoc-in [:ui-state :view-scope] view-scope))}))

(rf/reg-event-fx ::change-modal
  (fn [{:keys [db]} [_ modal modal-scope]]
    {:db (-> db
             (assoc-in [:ui-state :modal] modal)
             (assoc-in [:ui-state :modal-scope] modal-scope))}))

(rf/reg-event-fx ::init-login
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/login-view] {:form {:email nil
                                                               :password nil}}]]]}))

(rf/reg-event-fx ::init-register
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/registration-view]]]]}))

(rf/reg-event-fx ::init-books
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/books-panel]]]]]}))

(rf/reg-event-fx ::init-book
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-book-edit
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-libraries
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:entities :libraries] nil)
     :fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/libraries-panel]]]]
          [:dispatch [::fetch-libraries]]]}))

(rf/reg-event-fx ::init-library
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-library-edit
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-users
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/users-panel]]]]]}))

(rf/reg-event-fx ::init-user
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-user-edit
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-orders
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/orders-panel]]]]]}))

(rf/reg-event-fx ::init-order
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-order-edit
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-stats
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/stats-panel]]]]]}))

(rf/reg-event-fx ::init-stat
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-stat-edit
  (fn [{:keys [db]} [_ uid]]
    {}))
