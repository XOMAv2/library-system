(ns service.frontend.events
  (:require [re-frame.core :as rf]
            [reitit.frontend.controllers :refer [apply-controllers]]
            [service.frontend.effects :as effects]
            [service.frontend.db :as db]
            [service.frontend.router :as routes]
            [service.frontend.views :as views]
            [service.frontend.forms :as forms]
            [utilities.core :refer [any-or-coll->coll]]
            [service.frontend.api.gateway :as gateway]))

(rf/reg-cofx ::gateway/uri
  (fn [coeffects _]
    (assoc coeffects :uri "http://localhost:3001")))

(rf/reg-cofx ::gateway/tokens-path
  (fn [coeffects _]
    (assoc coeffects :tokens-path :tokens)))

(rf/reg-event-fx ::emit-coeffect
  (fn [_ [_ coeffect args]]
    {:fx [[coeffect args]]}))

(rf/reg-event-fx ::assoc-in-db
  (fn [{:keys [db]} [_ path value]]
    {:db (assoc-in db (any-or-coll->coll path) value)}))

(rf/reg-event-fx ::assoc-in-db-entites
  (fn [{:keys [db]} [_ path entities]]
    {:db (->> entities
              (map #(vector (:uid %) %))
              (into {})
              (assoc-in db (any-or-coll->coll path)))}))

(rf/reg-event-fx ::assoc-in-db-entity
  (fn [{:keys [db]} [_ path entity]]
    {:db (assoc-in db (conj (any-or-coll->coll path) (:uid entity)) entity)}))

(rf/reg-event-fx ::form-submit
  (fn [_ [_ form-path event-to-dispatch]]
    (when form-path
      {:fx [[:dispatch [::forms/explain-form form-path]]
            [:dispatch [::forms/set-form-submitted? form-path true]]
            [:dispatch [::form-valid? form-path event-to-dispatch]]]})))

(rf/reg-event-fx ::form-valid?
  (fn [{:keys [db]} [_ form-path event-to-dispatch]]
    (when form-path
      (let [form-path (any-or-coll->coll form-path)
            form-errors (get-in db (conj form-path :errors))]
        (when (empty? form-errors)
          {:fx [[:dispatch [::forms/set-form-loading? form-path true]]
                [:dispatch event-to-dispatch]]})))))

(rf/reg-event-fx ::form-failure
  (fn [_ [_ form-path response]]
    (when form-path
      {:fx [[:dispatch [::forms/set-form-loading? form-path false]]
            [:dispatch [::forms/set-form-disabled? form-path false]]
            [::effects/show-alert (or (-> response :response :message)
                                      (-> response :status-text))]
            (cond
              (= 401 (:status response)) [::effects/navigate {:route ::routes/login}]
              (= 403 (:status response)) [::effects/navigate {:route ::routes/books}])]})))

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
          [:dispatch [::change-view [views/login-view]]]]}))

(rf/reg-event-fx ::login-success
  (fn [{:keys [db]} [_ form-path {:keys [tokens payload]}]]
    (when form-path
      {:db (assoc db
                  :tokens tokens
                  :user-uid (:uid payload)
                  #_#_:user-role (:uid payload))
       :fx [[:dispatch [::navigate {:route ::routes/books}]]]})))

(rf/reg-event-fx ::init-register
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/registration-view]]]]}))


(rf/reg-event-fx ::registration-success
  (fn [_ [_ form-path user]]
    (when form-path
      {:fx [[:dispatch [::assoc-in-db-entity [:entities :users] user]]
            [:dispatch [::navigate {:route ::routes/login}]]]})))

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
          [:dispatch [::change-view [views/navigation-view [views/libraries-panel]]]]]}))

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
