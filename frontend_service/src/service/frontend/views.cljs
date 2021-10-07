(ns service.frontend.views
  (:require [re-frame.core :as rf]
            [malli.core :as m]
            [clojure.string]
            [utilities.schemas :as schemas]
            [reagent.core :as reagent]
            [service.frontend.subs :as subs]
            [service.frontend.config :as config]
            [service.frontend.forms :as forms]
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

(defn three-dot-card-layer [{:keys [class]}]
  [:div {:class ["h-full w-full z-10 absolute flex justify-center items-center"
                 "rounded-xl backdrop-blur-sm backdrop-brightness-95"]}
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
       (for [e errors]
         ^{:key [form-path field-path e]}
         [:p.text-red-500.text-sm.font-medium e]))]))

(def card-style
  "shadow-md rounded-xl p-6 bg-white w-[26rem]")

(defn login-form [{:keys [form-path]} submit-button]
  (let [schema [:map
                [:email schemas/non-empty-string]
                [:password schemas/non-empty-string]]
        explainer (m/explainer schema)]
    (reagent/create-class
     {:component-did-mount
      (fn [_]
        (rf/dispatch [::forms/set-form-explainer form-path explainer]))

      :reagent-render
      (fn [{:keys [form-path]} submit-button]
        (let [loading? @(rf/subscribe [::forms/get-form-loading? form-path])]
          [:div.relative
           (when loading?
             [three-dot-card-layer])
           [:div {:class card-style}

            [:form.space-y-4 {:on-change #(rf/dispatch [::forms/explain-form form-path])
                              :on-submit #(.preventDefault %)}
             [:h2.text-center.text-3xl.font-extrabold.text-gray-900
              "Log in to your account"]
             [input {:label "Email"
                     :type "email"
                     :form-path form-path
                     :field-path :email}]
             [input {:label "Password"
                     :type "password"
                     :form-path form-path
                     :field-path :password}]
             [:div.flex.justify-between.items-center
              submit-button
              [:a {:class "px-1 font-medium hover:underline text-blue-500"
                   :href (href ::routes/register)}
               "Go to registration"]]]]]))})))

(defn login-view []
  (let [form-path [:ui-state :view-scope :login-form]
        disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])]
    [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
     [login-form {:form-path form-path}
      [:button {:class button-style
                :type "submit"
                :on-click #(do #_"In case of a cold start of the application from the login screen, the explainer does not load."
                               (let [schemas [:map
                                              [:email schemas/non-empty-string]
                                              [:password schemas/non-empty-string]]
                                     explainer (m/explainer schemas)
                                     _ (rf/dispatch [::forms/set-form-explainer form-path explainer])])
                               (rf/dispatch [::events/login-form-submit form-path]))
                :disabled (when disabled? true)}
       "Log in"]]]))

(defn registration-form [{:keys [form-path]} submit-button]
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
    (reagent/create-class
     {:component-did-mount
      (fn [_]
        (rf/dispatch [::forms/set-form-explainer form-path explainer]))

      :reagent-render
      (fn [{:keys [form-path]} submit-button]
        (let [loading? @(rf/subscribe [::forms/get-form-loading? form-path])]
          [:div.relative
           (when loading?
             [three-dot-card-layer])
           [:div {:class card-style}
            [:form.space-y-4 {:on-change #(rf/dispatch [::forms/explain-form form-path])
                              :on-submit #(.preventDefault %)}
             [:h2.text-center.text-3xl.font-extrabold.text-gray-900
              "Register new account"]
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
                     :field-path :password-repeat}]
             [:div.flex.justify-between.items-center
              submit-button
              [:a {:class "px-1 font-medium hover:underline text-blue-500"
                   :href (href ::routes/login)}
               "Go to log in"]]]]]))})))

(defn registration-view []
  (let [form-path [:ui-state :view-scope :registration-form]
        disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])]
    [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
     [registration-form {:form-path form-path}
      [:button {:class button-style
                :type "submit"
                :on-click #(rf/dispatch [::events/registration-form-submit form-path])
                :disabled (when disabled? true)}
       "Register"]]]))

(defn modal-view [& forms]
  [:div {:class ["h-full w-full z-30 absolute flex justify-center items-center"
                 "backdrop-blur backdrop-brightness-90"]
         :on-click #(.log js/console "kek")}
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
  [:div.h-full.flex.flex-col.relative
   [:div.px-1.absolute.z-20.bottom-2.right-2
    [add-button {:text "Add library" :on-click identity}]]
   [:div.px-1
    [:input {:type "text" :class [input-style "w-[30rem]"]}]
    [:div.h-2]]
   [:div.overflow-y-auto.flex-grow
    [:ul.space-y-2.p-1
     (for [i (range 0 100)]
       ^{:key i}
       [:li [:a {:class ["w-[30rem] bg-blue-50 rounded-xl space-y-1 py-2 px-3"
                         "hover:bg-blue-200 focus:ring-2 focus:ring-offset-2"
                         "focus:ring-blue-500 focus:outline-none block"]
                 :href "#"}
             [:div.flex.flex-row.justify-between.items-center
              [:h1.font-normal.truncate {:class "text-2xl"} "Библиотека имени Ленина"]
              [:div.flex.flex-row.gap-1
               [:button {:class icon-button-style}
                [icons/pencil]]
               [:button {:class (class-concat icon-button-style
                                              "hover:text-red-500 focus:text-red-500")}
                [icons/trash {:class "stroke-current"}]]]]
             [:p.font-medium.truncate.leading-snug "ул. Бажова, дом 3, корпус 2"]
             [:ul
              (for [[index item] (map-indexed #(vector % %2) schedule)]
                [:li [:p {:class ["text-sm text-gray-500 font-light"
                                  (when (> index 0) "leading-tight")]}
                      item]])]]])]]])

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
