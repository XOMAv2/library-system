(ns service.frontend.events
  (:require [re-frame.core :as rf]
            [reitit.frontend.controllers :refer [apply-controllers]]
            [service.frontend.effects :as effects]
            [service.frontend.db :as db]
            [service.frontend.router :as routes]
            [service.frontend.views :as views]
            [service.frontend.forms :as forms]
            [cljs.reader :refer [read-string]]
            [utilities.core :refer [dissoc-in]]
            [utilities.core :refer [any-or-coll->coll]]
            [service.frontend.api.gateway :as gateway]))

(rf/reg-cofx ::local-storage
  (fn [coeffects key]
    (->> (name key)
         (.getItem js/localStorage)
         (read-string)
         (assoc-in coeffects [:local-storage key]))))

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
    {:db (assoc-in db (cons :entities (any-or-coll->coll path)) value)}))

(rf/reg-event-fx ::assoc-in-db-entities
  (fn [{:keys [db]} [_ path entities]]
    {:db (->> entities
              (map #(vector (:uid %) %))
              (into {})
              (assoc-in db (cons :entities (any-or-coll->coll path))))}))

(rf/reg-event-fx ::assoc-in-db-entity
  (fn [{:keys [db]} [_ path entity]]
    {:db (assoc-in db (concat [:entities]
                              (any-or-coll->coll path)
                              [(:uid entity)]) entity)}))

(rf/reg-event-fx ::dissoc-in-db-entities
  (fn [{:keys [db]} [_ path]]
    {:db (dissoc-in db (cons :entities (any-or-coll->coll path)))}))

(rf/reg-event-fx ::dissoc-in-db-entity
  (fn [{:keys [db]} [_ path entity]]
    {:db (dissoc-in db (concat [:entities]
                               (any-or-coll->coll path)
                               [(:uid entity)]))}))

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
            [:dispatch [::http-failure response]]]})))

(rf/reg-event-fx ::http-failure
  (fn [_ [_ response]]
    {:fx (->> (case (:status response)
                401 [[::effects/local-storage [:tokens nil]]
                     [::effects/local-storage [:credentials nil]]
                     [::effects/navigate {:route ::routes/login}]]
                403 [[::effects/navigate {:route ::routes/books}]]
                nil)
              (into [[::effects/show-alert (or (-> response :response :message)
                                               (-> response :status-text))]
                     [::effects/console-log response]]))}))

(rf/reg-event-fx ::init-db
  [(rf/inject-cofx ::local-storage :tokens)
   (rf/inject-cofx ::local-storage :credentials)]
  (fn [{:keys [local-storage]} _]
    {:db (-> db/default-db
             (assoc :tokens (-> local-storage :tokens))
             (assoc :user-uid (-> local-storage :credentials :user-uid))
             (assoc :user-role (-> local-storage :credentials :user-role)))}))

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
                  :user-role (:role payload))
       :fx [[:dispatch [::navigate {:route ::routes/books}]]
            [::effects/local-storage [:tokens tokens]]
            [::effects/local-storage [:credentials payload]]]})))

(rf/reg-event-fx ::init-register
  (fn [_ _]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/registration-view]]]]}))


(rf/reg-event-fx ::registration-success
  (fn [_ [_ form-path user]]
    (when form-path
      {:fx [[:dispatch [::assoc-in-db-entity :users user]]
            [:dispatch [::navigate {:route ::routes/login}]]]})))

(rf/reg-event-fx ::init-books
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:entities :books] nil)
     :fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/books-panel]]]]
          [:dispatch [::gateway/get-all-books
                      [::get-all-books-success]
                      [::http-failure]]]]}))

(rf/reg-event-fx ::get-all-books-success
  (fn [{:keys [db]} [_ {:keys [books]}]]
    {:dispatch [::assoc-in-db-entities :books books]}))

(rf/reg-event-fx ::init-book
  (fn [{:keys [db]} [_ uid]]
    (if-let [book (get-in db [:entities :books uid])]
      {:dispatch [::init-book-success uid book]}
      {:dispatch [::gateway/get-book [::init-book-edit-success uid] [::http-failure] uid]})))

(rf/reg-event-fx ::init-book-success
  (fn [_ [_ uid book]]
    {:fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/book-panel {:value book :uid uid}]]]]]}))

(rf/reg-event-fx ::init-book-add
  (fn [{:keys [db]} _]
    {:fx [[:dispatch [::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/books}]}
                                      [views/book-add-form {:form-path [:ui-state :modal-scope :add-book-form]}]]]]
          [:dispatch [::change-view [views/navigation-view [views/books-panel]]]]]}))

(rf/reg-event-fx ::book-add-success
  (fn [_ [_ form-path book]]
    (when form-path
      {:fx [[:dispatch [::assoc-in-db-entity :books book]]
            [:dispatch [::navigate {:route ::routes/books}]]]})))

(rf/reg-event-fx ::init-book-edit
  (fn [{:keys [db]} [_ uid]]
    (if-let [book (get-in db [:entities :books uid])]
      {:dispatch [::init-book-edit-success uid book]}
      {:dispatch [::gateway/get-book [::init-book-edit-success uid] [::http-failure] uid]})))

(rf/reg-event-fx ::init-book-edit-success
  (fn [_ [_ uid book]]
    {:fx [[:dispatch [::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/books}]}
                                      [views/book-edit-form {:form-path [:ui-state :modal-scope :edit-book-form]
                                                                :form-value book}]]]]
          [:dispatch [::change-view [views/navigation-view [views/books-panel]]]]]}))

(rf/reg-event-fx ::book-edit-success
  (fn [_ [_ form-path book]]
    (when form-path
      {:fx [[:dispatch [::assoc-in-db-entity :books book]]
            [:dispatch [::navigate {:route ::routes/books}]]]})))

(rf/reg-event-fx ::init-libraries
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:entities :libraries] nil)
     :fx [[:dispatch [::change-modal]]
          [:dispatch [::change-view [views/navigation-view [views/libraries-panel]]]]
          [:dispatch [::gateway/get-all-libraries
                      [::get-all-libraries-success]
                      [::http-failure]]]]}))

(rf/reg-event-fx ::get-all-libraries-success
  (fn [{:keys [db]} [_ {:keys [libraries]}]]
    {:dispatch [::assoc-in-db-entities :libraries libraries]}))

(rf/reg-event-fx ::init-library-add
  (fn [{:keys [db]} _]
    {:fx [[:dispatch [::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/libraries}]}
                                      [views/library-add-form {:form-path [:ui-state :modal-scope :add-library-form]}]]]]
          [:dispatch [::change-view [views/navigation-view [views/libraries-panel]]]]]}))

(rf/reg-event-fx ::library-add-success
  (fn [_ [_ form-path library]]
    (when form-path
      {:fx [[:dispatch [::assoc-in-db-entity :libraries library]]
            [:dispatch [::navigate {:route ::routes/libraries}]]]})))

(rf/reg-event-fx ::init-library
  (fn [{:keys [db]} [_ uid]]
    (if-let [library (get-in db [:entities :libraries uid])]
      {:dispatch [::init-library-success uid library]}
      {:dispatch [::gateway/get-library [::init-library-success uid] [::http-failure] uid]})))

(rf/reg-event-fx ::init-library-success
  (fn [_ [_ uid library]]
    {:fx [[:dispatch [::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/libraries}]}
                                      [views/library-disabled-form {:form-path [:ui-state :modal-scope :disabled-library-form]
                                                                    :form-value library}]]]]
          [:dispatch [::change-view [views/navigation-view [views/libraries-panel]]]]]}))

(rf/reg-event-fx ::init-library-edit
  (fn [{:keys [db]} [_ uid]]
    (if-let [library (get-in db [:entities :libraries uid])]
      {:dispatch [::init-library-edit-success uid library]}
      {:dispatch [::gateway/get-library [::init-library-edit-success uid] [::http-failure] uid]})))

(rf/reg-event-fx ::init-library-edit-success
  (fn [_ [_ uid library]]
    {:fx [[:dispatch [::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/libraries}]}
                                      [views/library-edit-form {:form-path [:ui-state :modal-scope :edit-library-form]
                                                                :form-value library}]]]]
          [:dispatch [::change-view [views/navigation-view [views/libraries-panel]]]]]}))

(rf/reg-event-fx ::library-edit-success
  (fn [_ [_ form-path library]]
    (when form-path
      {:fx [[:dispatch [::assoc-in-db-entity :libraries library]]
            [:dispatch [::navigate {:route ::routes/libraries}]]]})))

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
