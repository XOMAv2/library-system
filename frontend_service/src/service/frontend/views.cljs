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

(defn sign-in-form [{:keys [value-path]}]
  [:div {:class card-style}
   [:form.space-y-4 {:on-change #(when config/debug? (.log js/console %))
                     :on-submit #(.preventDefault %)}
    [:h2.text-center.text-3xl.font-extrabold.text-gray-900
     "Sign in to your account"]
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
      "Sign in"]
     [:a {:class "px-1 font-medium hover:underline text-blue-500"
          :href "#"}
      "Go to registration"]]]])

(defn sign-in-view []
  [:div.h-screen.bg-gradient-to-r.from-green-100.to-blue-200.flex.items-center.justify-center
   [sign-in-form]])

(defn main-panel []
  (let [name (rf/subscribe [::subs/name])]
    [:div
     [:h1
      "Hello from " @name]]))
