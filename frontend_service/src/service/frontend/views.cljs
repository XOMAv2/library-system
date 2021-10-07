(ns service.frontend.views
  (:require [re-frame.core :as rf]
            [malli.core :as m]
            [clojure.string]
            [utilities.schemas :as schemas]
            [reagent.core :as reagent]
            [utilities.core :refer [class-concat any-or-coll->coll remove-nth dissoc-in]]
            [service.frontend.subs :as subs]
            [service.frontend.config :as config]
            [service.frontend.forms :as forms]
            [service.frontend.api.gateway :as-alias gateway]
            [service.frontend.icons.outline :as icons]
            [service.frontend.events :as-alias events]
            [service.frontend.router :as-alias routes]
            [reitit.frontend.easy :refer [href]]))

(def input-style
  "block w-full py-2 px-3 border-2 border-blue-500 text-sm
   font-medium rounded-md focus:ring-2 focus:ring-offset-2
   focus:ring-blue-500 focus:outline-none bg-blue-50
   disabled:bg-gray-100 disabled:text-gray-500")

(def icon-button-style
  "rounded-xl transform hover:scale-125 focus:scale-125 focus:outline-none
   disabled:text-gray-500"
  #_"TODO: tailwind combine prefixes"
  #_"hover:disabled:scale-100")

(def button-style
  "py-2 px-4 text-sm font-medium rounded-md text-white
   bg-blue-500 hover:bg-blue-600 focus:outline-none
   focus:ring-2 focus:ring-offset-2 focus:ring-blue-500
   disabled:bg-blue-400")

(def outline-button-style
  "py-2 px-3 text-sm rounded-md
   font-medium focus:ring-2 focus:ring-offset-2
   focus:ring-blue-500 focus:outline-none
   bg-blue-50 hover:bg-blue-200
   disabled:bg-gray-100 disabled:text-gray-500")

(def card-style
  "shadow-md rounded-xl p-6 bg-white w-[26rem]")

(defn three-dot-loader [{:keys [class]}]
  [:div.flex
   [:div {:class (class-concat "h-3 w-3 rounded-full animate-bounce mr-1" class)}]
   [:div {:class (class-concat "h-3 w-3 rounded-full animate-bounce200 mr-1" class)}]
   [:div {:class (class-concat "h-3 w-3 rounded-full animate-bounce400" class)}]])

(defn three-dot-card-layer [{:keys [class modal?]}]
  [:div {:class (class-concat "h-full w-full absolute flex justify-center items-center"
                              "rounded-xl backdrop-blur-sm backdrop-brightness-95"
                              (if modal? "z-40" "z-10"))}
   [three-dot-loader {:class (class-concat "bg-blue-500" class)}]])

(defn loader-button [{:keys [text]}]
  [:button {:class button-style}
   [:div.flex.flex-row.items-center
    [:span.mr-2 text]
    [three-dot-loader {:class "bg-white"}]]])

(defn input [{:keys [label class type form-path field-path]}]
  (let [value @(rf/subscribe [::forms/get-field-value form-path field-path])
        errors @(rf/subscribe [::forms/get-field-errors form-path field-path])
        submitted? @(rf/subscribe [::forms/get-form-submitted? form-path])
        disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])]
    [:div.space-y-2
     [:label.font-medium label
      [:input {:class (class-concat input-style (when label "mt-2") class)
               :disabled (when disabled? true)
               :type type
               :value value
               :on-change #(rf/dispatch [::forms/set-field-value form-path field-path (-> % .-target .-value)])}]]
     (when submitted?
       (for [e (when (coll? errors) errors)]
         ^{:key [form-path field-path e]}
         [:p.text-red-500.text-sm.font-medium e]))]))

(defn sequential-input [{:keys [label form-path field-path]} [input-component input-props]]
  (let [errors @(rf/subscribe [::forms/get-field-errors form-path field-path])
        submitted? @(rf/subscribe [::forms/get-form-submitted? form-path])
        disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])]
    (let [value @(rf/subscribe [::forms/get-field-value form-path field-path])]
      [:div.space-y-2
       [:label.font-medium label
        (for [[index item] (map-indexed #(vector % %2) value)]
          ^{:key [form-path field-path index]}
          [:div.relative.mt-2
           [input-component (-> input-props
                                (assoc :form-path form-path)
                                (assoc :field-path (conj (any-or-coll->coll field-path) index)))]
           [:div.absolute.top-2.right-2
            [:button {:class (class-concat icon-button-style
                                           "flex-none hover:text-red-500 focus:text-red-500")
                      :on-click #(rf/dispatch
                                  [::forms/update-form-value
                                   form-path (fn [m]
                                               (if field-path
                                                 (let [path (any-or-coll->coll field-path)
                                                       update-fn (fn [c] (vec (remove-nth index c)))
                                                       m (update-in m path update-fn)
                                                       new-val-empty? (empty? (get-in m path))
                                                       m (if new-val-empty? (dissoc-in m path) m)]
                                                   m)
                                                 m))])
                      :disabled (when disabled? true)}
             [icons/trash {:class "stroke-current"}]]]])
        [:div.mt-2
         [:button {:class (class-concat outline-button-style "w-full")
                   :on-click #(rf/dispatch [::forms/update-field-value
                                            form-path field-path
                                            (fn [m] (vec (conj m nil)))])
                   :disabled (when disabled? true)}
          [:div.flex.justify-center.items-center
           [icons/plus]]]]]
       (when submitted?
         (for [e (filter #(and (some? %) (not (coll? %))) errors)]
           ^{:key [form-path field-path e]}
           [:p.text-red-500.text-sm.font-medium e]))])))

(defn form
  [{:keys [form-path form-value title submit-name event-ctor
           footer explainer disabled? on-submit]} & inputs]
  (reagent/create-class
   {:component-did-mount
    (fn [_]
      (rf/dispatch [::forms/set-form-value form-path (or form-value {})])
      (rf/dispatch [::forms/set-form-explainer form-path explainer])
      (rf/dispatch [::forms/set-form-disabled? form-path disabled?]))

    :reagent-render
    (fn [{:keys [form-path title submit-name event-ctor
                 footer explainer disabled? on-submit]} & inputs]
      (let [value @(rf/subscribe [::forms/get-form-value form-path])
            errors @(rf/subscribe [::forms/get-form-level-errors form-path])
            submitted? @(rf/subscribe [::forms/get-form-submitted? form-path])
            disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])
            modal? @(rf/subscribe [::subs/modal?])
            loading? @(rf/subscribe [::forms/get-form-loading? form-path])]
        [:div.relative
         (when loading?
           [three-dot-card-layer {:modal? modal?}])
         [:div {:class card-style}

          [:form.space-y-4 {:on-change #(rf/dispatch [::forms/explain-form form-path])
                            :on-submit #(.preventDefault %)}
           (when title
             [:h2.text-center.text-3xl.font-extrabold.text-gray-900
              title])
           #_"TODO: form body overflow scroll"
           #_[:div.space-y-4.overflow-y-auto.p-1.max-h-full #_{:class "max-h-[35rem]"}
              inputs]
           (for [input inputs]
             ^{:key [form-path input]} input)
           (when (or submit-name footer)
             [:div.flex.justify-between.items-center
              (when submit-name
                [:button {:class button-style
                          :type "submit"
                          :on-click (if on-submit
                                      #(on-submit value)
                                      #(do (.log js/console "submit" value)
                                           (rf/dispatch [::events/form-submit
                                                         form-path
                                                         (if event-ctor
                                                           (event-ctor value)
                                                           [::events/form-failure form-path])])))
                          :disabled (when disabled? true)}
                 submit-name])
              footer])
           (when submitted?
             (for [e (when (coll? errors) errors)]
               ^{:key [form-path e]}
               [:p.text-red-500.text-sm.font-medium e]))]]]))}))

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
           :footer [:a {:class "px-1 font-medium hover:underline text-blue-500"
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
  [:div {:class ["h-full w-full z-30 absolute flex justify-center items-center"
                 "backdrop-blur backdrop-brightness-90"]
         :on-click (when on-close-event
                     #(rf/dispatch on-close-event))}
   [:div {:on-click #(.stopPropagation %)}
    [:<> forms]]])

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
  [:button {:class (class-concat (clojure.string/replace button-style #"\srounded-md\s" " ")
                                 "shadow-3xl text-lg rounded-full")
            :type "button"
            :on-click on-click}
   [:div.flex.items-center
    [icons/plus]
    [:span.ml-2 text]]])

(defn libraries-panel []
  (let [libraries @(rf/subscribe [::subs/libraries])]
    [:div.h-full.flex.flex-col.relative
     [:div.px-1.absolute.z-20.bottom-2.right-2
      [add-button {:text "Add library" :on-click #(rf/dispatch [::events/navigate {:route ::routes/library-add}])}]]
     [:div.px-1
      [:input {:type "text" :class [input-style "w-[30rem]"]}]
      [:div.h-2]]
     [:div.overflow-y-auto.flex-grow
      [:ul.space-y-2.p-1
       (for [[uid library] libraries]
         ^{:key uid}
         [:li [:a {:class ["w-[30rem] bg-blue-50 rounded-xl space-y-1 py-2 px-3"
                           "hover:bg-blue-200 focus:ring-2 focus:ring-offset-2"
                           "focus:ring-blue-500 focus:outline-none block"]
                   :href (href ::routes/library {:uid uid})}
               [:div.flex.flex-row.justify-between.items-center
                [:h1.font-normal.truncate {:class "text-2xl"} (:name library)]
                [:div.flex.flex-row.gap-1
                 [:button {:class icon-button-style
                           :on-click #(do (.preventDefault %)
                                          (rf/dispatch [::events/navigate {:route ::routes/library-edit
                                                                           :path-params {:uid uid}}]))}
                  [icons/pencil]]
                 [:button {:class (class-concat icon-button-style
                                                "hover:text-red-500 focus:text-red-500")
                           :on-click #(do (.preventDefault %)
                                          (rf/dispatch [::gateway/delete-library
                                                        [::events/dissoc-in-db-entity :libraries]
                                                        [::events/http-failure]
                                                        uid]))}
                  [icons/trash {:class "stroke-current"}]]]]
               [:p.font-medium.truncate.leading-snug (:address library)]
               [:ul
                (for [[index item] (map-indexed #(vector % %2) (:schedule library))]
                  ^{:key [uid index item]}
                  [:li [:p {:class ["text-sm text-gray-500 font-light"
                                    (when (> index 0) "leading-tight")]}
                        item]])]]])]]]))

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

(defn books-panel []
  [:div "books"])

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
         ^{:key section}
         [:li [:a {:class (if (= route current-route-name)
                            (class-concat outline-button-style "block"
                                          "bg-gradient-to-r from-blue-200 to-green-100"
                                          "hover:to-blue-200")
                            (class-concat outline-button-style "block"))
                   :href (href route path-params query-params)}
               [:div.flex.items-center
                [icon]
                [:span.ml-2 section]]]])]]
     [:div.py-3.pr-3.flex-grow
      [:<> forms]]]))

(defn main-panel []
  (let [view @(rf/subscribe [::subs/current-view])
        modal @(rf/subscribe [::subs/current-modal])]
    [:div.relative
     modal
     view]))
