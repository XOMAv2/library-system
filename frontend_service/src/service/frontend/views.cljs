(ns service.frontend.views
  (:require [re-frame.core :as rf]
            [malli.core :as m]
            [clojure.string]
            [utilities.schemas :as schemas]
            [reagent.core :as reagent]
            [utilities.core :refer [class-concat any-or-coll->coll]]
            [service.frontend.subs :as subs]
            [service.frontend.config :as config]
            [service.frontend.forms :as forms]
            [service.frontend.api.gateway :as-alias gateway]
            [service.frontend.icons.outline :as icons]
            [service.frontend.events :as-alias events]
            [service.frontend.router :as-alias routes]
            [reitit.frontend.easy :refer [href]]
            [service.frontend.ui.basic :as basic :refer [input form select sequential-input]]
            [service.frontend.ui.styles :as styles]))

(defn login-form [{:keys [form-path]}]
  (let [schema [:map
                [:email schemas/non-empty-string]
                [:password schemas/non-empty-string]]
        explainer (m/explainer schema)
        event-ctor (fn [form-value]
                     [::gateway/get-tokens
                      [::events/login-success form-path]
                      [::events/form-failure form-path]
                      (:email form-value) (:password form-value)])]
    [form {:form-path form-path
           :title "Log in to your account"
           :submit-name "Log in"
           :event-ctor event-ctor
           #_"TODO: In case of a cold start of the application from the login screen, the explainer does not load."
           :on-submit (fn [form-value]
                        (do (rf/dispatch [::forms/set-form-explainer form-path explainer])
                            (rf/dispatch [::events/form-submit form-path (event-ctor form-value)])))
           :footer [:a {:class (class-concat styles/link-style "px-1 font-medium")
                        :href (href ::routes/register)}
                    "Go to registration"]
           :explainer explainer}
     [input {:label "Email"
             :type "email"
             :form-path form-path
             :field-path :email}]
     [input {:label "Password"
             :type "password"
             :form-path form-path
             :field-path :password}]]))

(defn login-view []
  [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
   [login-form {:form-path [:ui-state :view-scope :login-form]}]])

(defn registration-form [{:keys [form-path]}]
  (let [schema [:and
                [:map
                 [:name schemas/non-empty-string]
                 [:email schemas/non-empty-string]
                 [:password schemas/non-empty-string]
                 [:password-repeat schemas/non-empty-string]]
                [:fn {:error/message "repeated password should match the original one"
                      :error/path [:password-repeat]}
                 (fn [{:keys [password password-repeat]}]
                   (= password password-repeat))]]
        explainer (m/explainer schema)]
    [form {:form-path form-path
           :title "Register new account"
           :submit-name "Register"
           :event-ctor (fn [form-value]
                         [::gateway/add-user
                          [::events/registration-success form-path]
                          [::events/form-failure form-path]
                          (-> form-value
                              (assoc :role "reader")
                              (dissoc :password-repeat))])
           :footer [:a {:class "px-1 font-medium hover:underline text-blue-500"
                        :href (href ::routes/login)}
                    "Go to log in"]
           :explainer explainer}
     [input {:label "Name"
             :type "text"
             :form-path form-path
             :field-path :name}]
     [input {:label "Email"
             :type "email"
             :form-path form-path
             :field-path :email}]
     [input {:label "Password"
             :type "password"
             :form-path form-path
             :field-path :password}]
     [input {:label "Password once again"
             :type "password"
             :form-path form-path
             :field-path :password-repeat}]]))

(defn registration-view []
  [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
   [registration-form {:form-path [:ui-state :view-scope :registration-form]}]])

(defn modal-view [{:keys [on-close-event]} & forms]
  [:div {:class ["h-full w-full z-30 absolute flex justify-center"
                 "backdrop-blur backdrop-brightness-90"]
         :on-click (when on-close-event
                     #(do (rf/dispatch on-close-event)
                          (.stopPropagation %)))}
   [:div.py-10.h-full.flex.items-center
    [:div.max-h-full.overflow-y-auto {:on-click #(.stopPropagation %)}
     (for [form forms]
       ^{:key form}
       [:div form])]]])

(def ^:private sections
  [[icons/user-circle  "My profile" []]
   [icons/users        "Users"      [::routes/users]]
   [icons/book-open    "Books"      [::routes/books]]
   [icons/library      "Libraries"  [::routes/libraries]]
   [icons/shopping-bag "Orders"     [::routes/orders]]
   [icons/trending-up  "Statistics" [::routes/stats]]])

(def ^:private schedule
  ["Пн: 10:00-19:00"
   "Пн: 10:00-19:00"
   "Пн: 10:00-19:00"
   "Пн: 10:00-19:00"
   "Пн: 10:00-19:00"])

(defn add-button [{:keys [text on-click]}]
  [:button {:class (class-concat "text-white"
                                 (clojure.string/replace styles/button-style #"\srounded-md\s" " ")
                                 "shadow-3xl text-lg rounded-full")
            :type "button"
            :on-click on-click}
   [:div.flex.items-center
    [icons/plus]
    [:span.ml-2 text]]])

(defn library-item [{:keys [uid value href] :or {uid nil}}]
  (let [uid (or uid (:uid value))]
    [:li [:a {:class (class-concat styles/entity-item-style "block")
              :href href}
          [:div.flex.flex-row.justify-between.items-center
           [:h1.font-normal.truncate.text-2xl (:name value)]
           [:div.flex.flex-row.gap-1
            [:button {:class styles/icon-button-style
                      :on-click #(do (.preventDefault %)
                                     (rf/dispatch [::events/navigate {:route ::routes/library-edit
                                                                      :path-params {:uid uid}}]))}
             [icons/pencil]]
            [:button {:class (class-concat styles/icon-button-style
                                           "hover:text-red-500 focus:text-red-500")
                      :on-click #(do (.preventDefault %)
                                     (rf/dispatch [::gateway/delete-library
                                                   [::events/dissoc-in-db-entity :libraries]
                                                   [::events/http-failure]
                                                   uid]))}
             [icons/trash {:class "stroke-current"}]]]]
          [:p.font-medium.truncate.leading-snug (:address value)]
          [:ul
           (for [[index item] (map-indexed #(vector % %2) (:schedule value))]
             ^{:key [uid index item]}
             [:li [:p {:class ["text-sm text-gray-500 font-light"
                               (when (> index 0) "leading-tight")]}
                   item]])]]]))

(defn libraries-panel []
  (let [libraries @(rf/subscribe [::subs/libraries])]
    [:div.h-full.flex.flex-col.relative
     [:div.px-1.absolute.z-20.bottom-2.right-2
      [add-button {:text "Add library" :on-click #(rf/dispatch [::events/navigate {:route ::routes/library-add}])}]]
     [:div.px-1
      [:input {:type "text" :class [styles/input-style "w-[30rem]"]}]
      [:div.h-2]]
     [:div.overflow-y-auto.flex-grow
      [:ul.space-y-2.p-1
       (for [[uid library] libraries]
         ^{:key uid}
         [:div {:class "w-[30rem]"}
          [library-item {:value library
                         :uid uid
                         :href (href ::routes/library {:uid uid})}]])]]]))

(defn library-generic-form [{:keys [form-path form-value title submit-name
                                    event-ctor explainer disabled?]}]
  [form {:form-path form-path
         :form-value form-value
         :title title
         :submit-name submit-name
         :event-ctor event-ctor
         :disabled? disabled?
         :explainer explainer}
   [input {:label "Name"
           :type "text"
           :form-path form-path
           :field-path :name}]
   [input {:label "Address"
           :type "text"
           :form-path form-path
           :field-path :address}]
   [sequential-input {:label "Schedule"
                      :form-path form-path
                      :field-path :schedule}
    [input {:type "text"}]]])

(defn library-add-form [{:keys [form-path]}]
  (let [explainer (m/explainer schemas/library-add)]
    [library-generic-form {:form-path form-path
                           :title "Add new library"
                           :submit-name "Add"
                           :event-ctor (fn [form-value]
                                         [::gateway/add-library
                                          [::events/library-add-success form-path]
                                          [::events/form-failure form-path]
                                          form-value])
                           :explainer explainer}]))

(defn library-edit-form [{:keys [form-path form-value]}]
  (let [explainer (m/explainer schemas/library-update)]
    [library-generic-form {:form-path form-path
                           :form-value form-value
                           :title "Edit existing library"
                           :submit-name "Edit"
                           :event-ctor (fn [form-value]
                                         [::gateway/update-library
                                          [::events/library-edit-success form-path]
                                          [::events/form-failure form-path]
                                          (:uid form-value)
                                          form-value])
                           :explainer explainer}]))

(defn library-disabled-form [{:keys [form-path form-value]}]
  [library-generic-form {:form-path form-path
                         :form-value form-value
                         :title "Library info"
                         :disabled? true}])

(defn book-generic-form [{:keys [form-path form-value title submit-name
                                 event-ctor explainer disabled?]}]
  [form {:form-path form-path
         :form-value form-value
         :title title
         :submit-name submit-name
         :event-ctor event-ctor
         :disabled? disabled?
         :explainer explainer}
   [input {:label "Name"
           :type "text"
           :form-path form-path
           :field-path :name}]
   [input {:label "Description"
           :type "text"
           :form-path form-path
           :field-path :description}]
   [sequential-input {:label "Authors"
                      :form-path form-path
                      :field-path :authors}
    [input {:type "text"}]]
   [sequential-input {:label "Genres"
                      :form-path form-path
                      :field-path :genres}
    [input {:type "text"}]]
   [input {:label "Price"
           :type "number"
           :form-path form-path
           :field-path :price
           :min "0"}]])

(defn book-add-form [{:keys [form-path]}]
  (let [explainer (m/explainer schemas/book-add)]
    [book-generic-form {:form-path form-path
                        :title "Add new book"
                        :submit-name "Add"
                        :event-ctor (fn [form-value]
                                      [::gateway/add-book
                                       [::events/book-add-success form-path]
                                       [::events/form-failure form-path]
                                       form-value])
                        :explainer explainer}]))

(defn book-edit-form [{:keys [form-path form-value]}]
  (let [explainer (m/explainer schemas/book-update)]
    [book-generic-form {:form-path form-path
                        :form-value form-value
                        :title "Edit existing book"
                        :submit-name "Edit"
                        :event-ctor (fn [form-value]
                                      [::gateway/update-book
                                       [::events/book-edit-success form-path]
                                       [::events/form-failure form-path]
                                       (:uid form-value)
                                       form-value])
                        :explainer explainer}]))

(defn book-item [{:keys [value uid href] :or {uid nil}}]
  (let [uid (or uid (:uid value))]
    [:li [:a {:class (class-concat styles/entity-item-style "block")
              :href href}
          [:div.space-y-1
           [:div.flex.flex-row.justify-between.items-center
            [:h1.font-normal.truncate.text-2xl (:name value)]
            [:div.flex.flex-row.gap-1
             [:button {:class styles/icon-button-style
                       :on-click #(do (.preventDefault %)
                                      (rf/dispatch [::events/navigate {:route ::routes/book-edit
                                                                       :path-params {:uid uid}}]))}
              [icons/pencil]]
             [:button {:class (class-concat styles/icon-button-style
                                            "hover:text-red-500 focus:text-red-500")
                       :on-click #(do (.preventDefault %)
                                      (rf/dispatch [::gateway/delete-book
                                                    [::events/dissoc-in-db-entity :books]
                                                    [::events/http-failure]
                                                    uid]))}
              [icons/trash {:class "stroke-current"}]]]]
           [:p.font-medium.overflow-ellipsis.overflow-hidden.leading-snug (:description value)]
           [:div.flex.flex-row.flex-wrap.gap-x-2.gap-y-0
            (for [author (:authors value)]
              ^{:key [uid author]}
              [:button.text-md.font-normal {:class styles/link-style}
               author])]
           [:div.flex.flex-row.flex-wrap.gap-2
            (for [genres (:genres value)]
              ^{:key [uid genres]}
              [:button {:class (class-concat styles/chip-style (rand-nth styles/chip-colors))}
               genres])]]
          [:div.flex.flex-row.justify-end
           [:span.text-sm "Price " [:span.font-normal.text-xl (:price value) " ₿"]]]]]))

(defn books-panel []
  (let [books @(rf/subscribe [::subs/books])]
    [:div.h-full.flex.flex-col.relative
     [:div.px-1.absolute.z-20.bottom-2.right-2
      [add-button {:text "Add book" :on-click #(rf/dispatch [::events/navigate {:route ::routes/book-add}])}]]
     [:div.px-1
      [:input {:type "text" :class [styles/input-style "w-[30rem]"]}]
      [:div.h-2]]
     [:div.overflow-y-auto.flex-grow
      [:ul.space-y-2.p-1
       (for [[uid book] books]
         ^{:key uid}
         [:div {:class "w-[30rem]"}
          [book-item {:value book
                      :uid uid
                      :href (href ::routes/library-books-by-book {:uid uid})}]])]]]))


#_(-> @(rf/subscribe [::subs/db]) :entities :libraries vals)

(defn users-panel []
  [:div "users"])

(defn orders-panel []
  [:div "orders"])

(defn stats-panel []
  [:div "statistics"])

(defn navigation-view [& forms]
  (let [current-route-name @(rf/subscribe [::subs/current-route-name])]
    [:div.flex.flex-row.h-screen
     [:nav.p-3.w-52.overflow-y-auto.flex-none #_"TODO: think about responsive design."
      [:ul.space-y-2
       (for [[icon section [route path-params query-params]] sections]
         ^{:key [route path-params query-params]}
         [:li [:a {:class (if (= route current-route-name)
                            (class-concat styles/outline-button-style "block"
                                          "bg-gradient-to-r from-blue-200 to-green-100"
                                          "hover:to-blue-200")
                            (class-concat styles/outline-button-style "block"))
                   :href (href route path-params query-params)}
               [:div.flex.items-center
                [icon]
                [:span.ml-2 section]]]])]]
     [:div.py-3.pr-3.flex-grow
      [:<> forms]
      #_(for [form forms]
        ^{:key form}
        [:div form])]]))

(defn main-panel []
  (let [view @(rf/subscribe [::subs/current-view])
        modal @(rf/subscribe [::subs/current-modal])]
    [:div.relative
     modal
     view]))
