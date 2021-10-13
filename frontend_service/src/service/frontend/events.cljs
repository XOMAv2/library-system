(ns service.frontend.events
  (:require [re-frame.core :as rf]
            [reitit.frontend.controllers :refer [apply-controllers]]
            [service.frontend.effects :as effects]
            [service.frontend.db :as db]
            [service.frontend.router :as routes]
            [service.frontend.views :as views]
            [service.frontend.forms :as forms]
            [service.frontend.config :as config]
            [cljs.reader :refer [read-string]]
            [utilities.core :refer [dissoc-in]]
            [utilities.time :as time]
            [day8.re-frame.async-flow-fx :as async-flow-fx]
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
    (assoc coeffects :uri config/gateway-uri)))

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

(rf/reg-event-fx ::dispatch-n
  (fn [_ [_ events]]
    {:dispatch-n events}))

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
             (assoc :user-uid (-> local-storage :credentials :uid))
             (assoc :user-role (-> local-storage :credentials :role)))}))

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

(rf/reg-event-fx ::init-library-books-by-book
  (fn [{:keys [db]} [_ {:keys [book-uid]}]]
    (let [book (get-in db [:entities :books book-uid])
          libraries (get-in db [:entities :libraries])
          library-books (get-in db [:entities :library-books])]
      (case [(some? book) (not-empty libraries) (not-empty library-books)]
        [true true true]
        {:dispatch [::init-library-books-by-book-success book-uid]}

        [true true false]
        {:async-flow {:first-dispatch [::gateway/get-all-library-books
                                       [::get-all-library-books-success]
                                       [::http-failure]]
                      :rules [{:when :seen?
                               :events ::gateway/get-all-library-books-success
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen?
                               :events ::gateway/get-all-library-books-failure
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        [true false true]
        {:async-flow {:first-dispatch [::gateway/get-all-libraries
                                       [::get-all-libraries-success :books]
                                       [::http-failure]]
                      :rules [{:when :seen?
                               :events ::gateway/get-all-libraries-success
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen?
                               :events ::gateway/get-all-libraries-failure
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        [true false false]
        {:async-flow {:first-dispatch [::dispatch-n
                                       [[::gateway/get-all-libraries
                                         [::get-all-libraries-success :books]
                                         [::http-failure]]
                                        [::gateway/get-all-library-books
                                         [::get-all-library-books-success]
                                         [::http-failure]]]]
                      :rules [{:when :seen-both?
                               :events [::gateway/get-all-libraries-success
                                        ::gateway/get-all-library-books-success]
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen-any-of?
                               :events [::gateway/get-all-libraries-failure
                                        ::gateway/get-all-library-books-failure]
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        [false true true]
        {:async-flow {:first-dispatch [::gateway/get-book
                                       [::assoc-in-db-entity :books]
                                       [::http-failure]
                                       book-uid]
                      :rules [{:when :seen?
                               :events ::gateway/get-book-success
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen?
                               :events ::gateway/get-book-failure
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        [false true false]
        {:async-flow {:first-dispatch [::dispatch-n
                                       [[::gateway/get-book
                                         [::assoc-in-db-entity :books]
                                         [::http-failure]
                                         book-uid]
                                       [::gateway/get-all-library-books
                                        [::get-all-library-books-success]
                                        [::http-failure]]]]
                      :rules [{:when :seen-both?
                               :events [::gateway/get-book-success
                                        ::gateway/get-all-library-books-success]
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen-any-of?
                               :events [::gateway/get-book-failure
                                        ::gateway/get-all-library-books-failure]
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        [false false true]
        {:async-flow {:first-dispatch [::dispatch-n
                                       [[::gateway/get-book
                                         [::assoc-in-db-entity :books]
                                         [::http-failure]
                                         book-uid]
                                        [::gateway/get-all-libraries
                                         [::get-all-libraries-success]
                                         [::http-failure]]]]
                      :rules [{:when :seen-both?
                               :events [::gateway/get-book-success
                                        ::gateway/get-all-libraries-success]
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen-any-of?
                               :events [::gateway/get-book-failure
                                        ::gateway/get-all-libraries-failure]
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        {:async-flow {:first-dispatch [::dispatch-n
                                       [[::gateway/get-book
                                         [::assoc-in-db-entity :books]
                                         [::http-failure]
                                         book-uid]
                                        [::gateway/get-all-libraries
                                         [::get-all-libraries-success]
                                         [::http-failure]]
                                        [::gateway/get-all-library-books
                                         [::get-all-library-books-success]
                                         [::http-failure]]]]
                      :rules [{:when :seen-all-of?
                               :events [::gateway/get-book-success
                                        ::gateway/get-all-libraries-success
                                        ::gateway/get-all-library-books-success]
                               :dispatch [::init-library-books-by-book-success book-uid]
                               :halt? true}
                              {:when :seen-any-of?
                               :events [::gateway/get-book-failure
                                        ::gateway/get-all-libraries-failure
                                        ::gateway/get-all-library-books-failure]
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}))))

(rf/reg-event-fx ::get-all-library-books-success
  (fn [_ [_ {:keys [library-books]}]]
    {:dispatch [::assoc-in-db-entities :library-books library-books]}))

(rf/reg-event-fx ::init-library-books-by-book-success
  (fn [{:keys [db]} [_ book-uid]]
    (let [book (-> db :entities :books (get book-uid))]
      {:dispatch-n [[::change-modal]
                    [::change-view [views/navigation-view [views/library-books-panel {:book book}]]]]})))

(rf/reg-event-fx ::init-library-book-by-book-add
  (fn [{:keys [db]} [_ {:keys [book-uid]}]]
    (let [book (get-in db [:entities :books book-uid])
          libraries (get-in db [:entities :libraries])]
      (cond
        (and book (not-empty libraries))
        {:dispatch [::init-library-book-by-book-add-success book-uid]}

        book #_"load libraries"
        {:async-flow {:first-dispatch [::gateway/get-all-libraries
                                       [::get-all-libraries-success]
                                       [::http-failure]]
                      :rules [{:when :seen?
                               :events ::gateway/get-all-libraries-success
                               :dispatch [::init-library-book-by-book-add-success book-uid]
                               :halt? true}
                              {:when :seen?
                               :events ::gateway/get-all-libraries-failure
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        (not-empty libraries) #_"load book"
        {:async-flow {:first-dispatch [::gateway/get-book
                                       [::assoc-in-db-entity :books]
                                       [::http-failure]
                                       book-uid]
                      :rules [{:when :seen?
                               :events ::gateway/get-book-success
                               :dispatch [::init-library-book-by-book-add-success book-uid]
                               :halt? true}
                              {:when :seen?
                               :events ::gateway/get-book-failure
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}

        :else #_"load both"
        {:async-flow {:first-dispatch [::dispatch-n
                                       [[::gateway/get-book
                                         [::assoc-in-db-entity :books]
                                         [::http-failure]
                                         book-uid]
                                        [::gateway/get-all-libraries
                                         [::get-all-libraries-success]
                                         [::http-failure]]]]
                      :rules [{:when :seen-both?
                               :events [::gateway/get-book-success
                                        ::gateway/get-all-libraries-success]
                               :dispatch [::init-library-book-by-book-add-success book-uid]
                               :halt? true}
                              {:when :seen-any-of?
                               :events [::gateway/get-book-failure
                                        ::gateway/get-all-libraries-failure]
                               :dispatch [::async-flow-fx/notify]
                               :halt? true}]}}))))

(rf/reg-event-fx ::init-library-book-by-book-add-success
  (fn [{:keys [db]} [_ book-uid]]
    (let [book (get-in db [:entities :books book-uid])]
      {:dispatch-n [[::change-modal
                     [views/modal-view {:on-close-event [::navigate {:route ::routes/library-books-by-book
                                                                     :path-params {:uid book-uid}}]}
                      [views/library-book-add-form {:form-path [:ui-state :modal-scope :add-library-book-by-book-form]
                                                    :book book}]]]
                    [::change-view [views/navigation-view [views/library-books-panel {:book book}]]]]})))

(rf/reg-event-fx ::library-book-by-book-add-success
  (fn [_ [_ form-path book-uid library-book]]
    (when form-path
      {:dispatch-n [[::assoc-in-db-entity :library-books library-book]
                    [::navigate {:route ::routes/library-books-by-book
                                 :path-params {:uid book-uid}}]]})))

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
  (fn [_ [_ {:keys [libraries]}]]
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
    {:async-flow {:first-dispatch [::dispatch-n
                                   [[::gateway/get-all-users        [::get-all-users-success]        [::http-failure]]
                                    [::gateway/get-all-user-limits  [::get-all-user-limits-success]  [::http-failure]]
                                    [::gateway/get-all-user-ratings [::get-all-user-ratings-success] [::http-failure]]]]
                  :rules [{:when :seen-all-of?
                           :events [::gateway/get-all-users-success
                                    ::gateway/get-all-user-limits-success
                                    ::gateway/get-all-user-ratings-success]
                           :dispatch [::init-users-success]
                           :halt? true}
                          {:when :seen-any-of?
                           :events [::gateway/get-all-users-failure
                                    ::gateway/get-all-user-limits-failure
                                    ::gateway/get-all-user-ratings-failure]
                           :dispatch [::async-flow-fx/notify]
                           :halt? true}]}}))

(rf/reg-event-fx ::init-users-success
  (fn [_ _]
    {:dispatch-n [[::change-modal]
                  [::change-view [views/navigation-view [views/users-panel]]]]}))

(rf/reg-event-fx ::get-all-user-limits-success
  (fn [_ [_ {:keys [limits]}]]
    {:dispatch [::assoc-in-db-entities :user-limits limits]}))

(rf/reg-event-fx ::get-all-user-ratings-success
  (fn [_ [_ {:keys [ratings]}]]
    {:dispatch [::assoc-in-db-entities :user-ratings ratings]}))

(rf/reg-event-fx ::init-user
  (fn [{:keys [db]} [_ uid]]
    (if-let [user (get-in db [:entities :users uid])]
      {:dispatch [::init-user-success uid user]}
      {:dispatch [::gateway/get-user [::init-user-success uid] [::http-failure] uid]})))

(rf/reg-event-fx ::init-user-success
  (fn [_ [_ uid user]]
    {:dispatch-n [[::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/users}]}
                                   [views/user-disabled-form {:form-path [:ui-state :modal-scope :disabled-user-form]
                                                              :form-value user}]]]
                  [::change-view [views/navigation-view [views/users-panel]]]]}))

(rf/reg-event-fx ::init-user-add
  (fn [_ _]
    {:dispatch-n [[::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/users}]}
                                   [views/user-add-form {:form-path [:ui-state :modal-scope :add-user-form]}]]]
                  [::change-view [views/navigation-view [views/users-panel]]]]}))

(rf/reg-event-fx ::user-add-success
  (fn [_ [_ user]]
    {:dispatch-n [[::assoc-in-db-entity :users user]
                  [::navigate {:route ::routes/users}]]}))

(rf/reg-event-fx ::init-user-edit
  (fn [{:keys [db]} [_ uid]]
    (if-let [user (get-in db [:entities :users uid])]
      {:dispatch [::init-user-edit-success uid user]}
      {:dispatch [::gateway/get-user [::init-user-edit-success uid] [::http-failure] uid]})))

(rf/reg-event-fx ::init-user-edit-success
  (fn [_ [_ uid user]]
    {:dispatch-n [[::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/users}]}
                                   [views/user-edit-form {:form-path [:ui-state :modal-scope :edit-user-form]
                                                          :form-value user}]]]
                  [::change-view [views/navigation-view [views/users-panel]]]]}))

(rf/reg-event-fx ::user-edit-success
  (fn [_ [_ user]]
    {:dispatch-n [[::assoc-in-db-entity :users user]
                  [::navigate {:route ::routes/users}]]}))

(rf/reg-event-fx ::init-orders
  (fn [_ _]
    {:async-flow {:first-dispatch [::dispatch-n
                                   [[::gateway/get-all-orders    [::get-all-orders-success]    [::http-failure]]
                                    [::gateway/get-all-libraries [::get-all-libraries-success] [::http-failure]]
                                    [::gateway/get-all-books     [::get-all-books-success]     [::http-failure]]
                                    [::gateway/get-all-users     [::get-all-users-success]     [::http-failure]]]]
                  :rules [{:when :seen-all-of?
                           :events [::gateway/get-all-orders-success
                                    ::gateway/get-all-libraries-success
                                    ::gateway/get-all-books-success
                                    ::gateway/get-all-users-success]
                           :dispatch [::init-orders-success]
                           :halt? true}
                          {:when :seen-any-of?
                           :events [::gateway/get-all-orders-failure
                                    ::gateway/get-all-libraries-failure
                                    ::gateway/get-all-books-failure
                                    ::gateway/get-all-users-failure]
                           :dispatch [::async-flow-fx/notify]
                           :halt? true}]}}))

(rf/reg-event-fx ::init-orders-success
  (fn [_ _]
    {:dispatch-n [[::change-modal]
                  [::change-view [views/navigation-view [views/orders-panel]]]]}))

(rf/reg-event-fx ::get-all-orders-success
  (fn [_ [_ {:keys [orders]}]]
    {:dispatch [::assoc-in-db-entities :orders orders]}))

(rf/reg-event-fx ::init-order
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-order-add
  (fn [{:keys [db]} [_ {:keys [book-uid library-uid]}]]
    (if (-> db :user-role (= "admin"))
      {:db (assoc-in db [:entities :users] nil)
       :dispatch [::gateway/get-all-users
                  [::init-order-add-success {:book-uid book-uid
                                             :library-uid library-uid}]
                  [::http-failure]]}
      (let [order {:book-uid book-uid
                   :library-uid library-uid
                   :user-uid (:user-uid db)
                   :booking-date (time/now)}]
         {:dispatch [::gateway/add-order
                     [::dispatch [::order-add-success]]
                     [::http-failure]
                     order]}))))

(rf/reg-event-fx ::init-order-add-success
  (fn [_ [_ {:keys [book-uid library-uid]} response]]
    {:fx [[:dispatch [::get-all-users-success response]]
          [:dispatch [::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/orders}]}
                                      [views/order-add-form {:form-path [:ui-state :modal-scope :add-order-form]
                                                             :book-uid book-uid
                                                             :library-uid library-uid}]]]]
          [:dispatch [::change-view [views/navigation-view [views/orders-panel]]]]]}))

(rf/reg-event-fx ::get-all-users-success
  (fn [_ [_ {:keys [users]}]]
    {:dispatch [::assoc-in-db-entities :users users]}))

(rf/reg-event-fx ::order-add-success
  (fn [_ [_ order]]
    {:dispatch-n [[::assoc-in-db-entity :orders order]
                  [::navigate {:route ::routes/orders}]]}))

(rf/reg-event-fx ::init-order-edit
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-book-return
  (fn [{:keys [db]} [_ {:keys [order-uid user-uid]}]]
    {:dispatch [::init-book-return-success {:order-uid order-uid
                                            :user-uid user-uid}]}))

(rf/reg-event-fx ::init-book-return-success
  (fn [_ [_ {:keys [order-uid user-uid]}]]
    {:dispatch-n [[::change-modal [views/modal-view {:on-close-event [::navigate {:route ::routes/orders}]}
                                   [views/book-return-form {:form-path [:ui-state :modal-scope :return-book-form]
                                                            :order-uid order-uid
                                                            :user-uid user-uid}]]]
                  [::change-view [views/navigation-view [views/orders-panel]]]]}))

(rf/reg-event-fx ::book-return-success
  (fn [_ [_ order]]
    {:dispatch-n [[::assoc-in-db-entity :orders order]
                  [::navigate {:route ::routes/orders}]]}))

(rf/reg-event-fx ::init-stats
  (fn [_ _]
    {:async-flow {:first-dispatch [::gateway/get-all-stat-records
                                   [::get-all-stat-records-success]
                                   [::http-failure]]
                  :rules [{:when :seen?
                           :events ::gateway/get-all-stat-records-success
                           :dispatch [::init-stats-success]
                           :halt? true}
                          {:when :seen?
                           :events ::gateway/get-all-stat-records-failure
                           :dispatch [::async-flow-fx/notify]
                           :halt? true}]}}))

(rf/reg-event-fx ::init-stats-success
  (fn [_ _]
    {:dispatch-n [[::change-modal]
                  [::change-view [views/navigation-view [views/stats-panel]]]]}))

(rf/reg-event-fx ::get-all-stat-records-success
  (fn [_ [_ {:keys [stats]}]]
    (.log js/console :stats stats)
    {:dispatch [::assoc-in-db-entities :stat-records stats]}))

(rf/reg-event-fx ::init-stat
  (fn [{:keys [db]} [_ uid]]
    {}))

(rf/reg-event-fx ::init-stat-edit
  (fn [{:keys [db]} [_ uid]]
    {}))
