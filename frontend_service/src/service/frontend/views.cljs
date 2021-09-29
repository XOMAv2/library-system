(ns service.frontend.views
  (:require [re-frame.core :as rf]
            [service.frontend.subs :as subs]))

(defn main-panel []
  (let [name (rf/subscribe [::subs/name])]
    [:div
     [:h1
      "Hello from " @name]]))
