(ns service.frontend.views
  (:require [re-frame.core :as rf]
            [service.frontend.subs :as subs]
            [service.frontend.config :as config]
            [service.frontend.icons.outline :as icons]))

(def input-style
  "block w-full py-2 px-3 border-2 border-blue-500 text-sm
   font-medium rounded-md focus:ring-2 focus:ring-offset-2
   focus:ring-blue-500 focus:outline-none bg-blue-50")

(def card-style
  "shadow-md rounded-xl p-6 bg-white w-[26rem]")

(defn login-form [{:keys [value-path]}]
  [:div {:class card-style}
   [:form.space-y-4 {:on-change #(when config/debug? (.log js/console %))
                     :on-submit #(.preventDefault %)}
    [:h2.text-center.text-3xl.font-extrabold.text-gray-900
     "Log in to your account"]
    [:div
     [:label.font-medium "Email"
      [:input {:class [input-style "mt-2"]
               :type "email"}]]]
    [:div
     [:label.font-medium "Password"
      [:input {:class [input-style "mt-2"]
               :type "password"}]]]
    [:div.flex.justify-between.items-center
     [:button {:class ["py-2 px-4 text-sm font-medium rounded-md text-white"
                       "bg-blue-500 hover:bg-blue-600 focus:outline-none"
                       "focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"]
               :type "button"}
      "Log in"]
     [:a {:class "px-1 font-medium hover:underline text-blue-500"
          :href "#"}
      "Go to registration"]]]])

(defn login-view []
  [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
   [login-form]])

(defn registration-form []
  [:div {:class card-style}
   [:form.space-y-4 {:on-change #(when config/debug? (.log js/console %))
                     :on-submit #(.preventDefault %)}
    [:h2.text-center.text-3xl.font-extrabold.text-gray-900
     "Register new account"]
    [:div
     [:label.font-medium "Name"
      [:input {:class [input-style "mt-2"]
               :type "text"}]]]
    [:div
     [:label.font-medium "Email"
      [:input {:class [input-style "mt-2"]
               :type "email"}]]]
    [:div
     [:label.font-medium "Password"
      [:input {:class [input-style "mt-2"]
               :type "password"}]]]
    [:div
     [:label.font-medium "Password once again"
      [:input {:class [input-style "mt-2"]
               :type "password"}]]]
    [:div.flex.justify-between.items-center
     [:button {:class ["py-2 px-4 text-sm font-medium rounded-md text-white"
                       "bg-blue-500 hover:bg-blue-600 focus:outline-none"
                       "focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"]
               :type "button"}
      "Register"]
     [:a {:class "px-1 font-medium hover:underline text-blue-500"
          :href "#"}
      "Go to log in"]]]])

(defn registration-view []
  [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
   [registration-form]])

(defn modal-view [& forms]
  [:div {:class ["h-full w-full z-20 absolute flex justify-center items-center"
                 "backdrop-blur backdrop-brightness-90"]
         :on-click #(.log js/console "kek")}
   [:div {:on-click #(.stopPropagation %)}
    [:<> forms]]])

(defn three-dot-loader [{:keys [class]}]
  (let [class (if (coll? class) class [class])]
  [:div.flex
   [:div {:class (concat ["h-2 w-2 rounded-full animate-bounce mr-1"] class)}]
   [:div {:class (concat ["h-2 w-2 rounded-full animate-bounce200 mr-1"] class)}]
   [:div {:class (concat ["h-2 w-2 rounded-full animate-bounce400"] class)}]]))

(defn loader-button [{:keys [text]}]
  [:button {:class ["flex flex-row items-center"
                    "py-2 px-4 text-sm font-medium rounded-md text-white"
                    "bg-blue-500 hover:bg-blue-600 focus:outline-none"
                    "focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"]}
   [:span.mr-2 text]
   [three-dot-loader {:class "bg-white"}]])

(def ^:private sections
  [[icons/user-circle "My profile" false]
   [icons/users "Users" false]
   [icons/book-open "Books" false]
   [icons/library "Libraries" true]
   [icons/shopping-bag "Orders" false]
   [icons/trending-up "Statistics" false]])

(def ^:private schedule
  ["Пн: 10:00-19:00"
   "Пн: 10:00-19:00"
   "Пн: 10:00-19:00"
   "Пн: 10:00-19:00"
   "Пн: 10:00-19:00"])

(defn add-button [{:keys [text on-click]}]
  [:button {:class ["shadow-3xl py-2 px-4 text-lg font-medium rounded-full text-white"
                    "bg-blue-500 hover:bg-blue-600 focus:outline-none flex items-center"
                    "focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"]
            :type "button"
            :on-click on-click}
   [icons/plus]
   [:span.ml-2 text]])

(defn libraries-panel []
  [:div.h-full.flex.flex-col.relative
   [:div.px-1.absolute.z-10.bottom-2.right-2
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
               [:button {:class ["rounded-xl transform hover:scale-125 focus:outline-none"
                                 "focus:scale-125"]}
                [icons/pencil]]
               [:button {:class ["rounded-xl transform hover:scale-125 hover:text-red-500"
                                 "focus:scale-125 focus:text-red-500 focus:outline-none"]}
                [icons/trash {:class "stroke-current"}]]]]
             [:p.font-medium.truncate.leading-snug "ул. Бажова, дом 3, корпус 2"]
             [:ul
              (for [[index item] (map-indexed #(vector % %2) schedule)]
                [:li [:p {:class ["text-sm text-gray-500 font-light"
                                  (when (> index 0) "leading-tight")]}
                      item]])]]])]]])

(defn navigation-view []
  (let [common-section-style ["py-2 px-3 text-sm flex items-center rounded-md"
                              "block font-medium focus:ring-2 focus:ring-offset-2"
                              "focus:ring-blue-500 focus:outline-none"]
        section-style (conj common-section-style
                            "bg-blue-50 hover:bg-blue-200")
        active-section-style (conj common-section-style
                                   "bg-gradient-to-r from-blue-200 to-green-100 hover:to-blue-200")]
    [:div.flex.flex-row.h-screen
     [:nav.p-3.w-52.overflow-y-auto.flex-none #_"TODO: think about responsive design."
      [:ul.space-y-2
       (for [[icon section active?] sections]
         ^{:key section}
         [:li [:a {:class (if active? active-section-style section-style)
                   :href "#"}
               [icon]
               [:span.ml-2 section]]])]]
     [:div.py-3.pr-3.flex-grow
      [libraries-panel]]]))

(defn main-panel []
  (let [name (rf/subscribe [::subs/name])]
    [:div
     [:h1
      "Hello from " @name]]))
