(ns service.frontend.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [service.frontend.router :refer [start-router!]]
            [service.frontend.events :as events]
            [service.frontend.views :as views]
            [service.frontend.config :as config]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (rf/dispatch-sync [::events/init-db])
  (dev-setup)
  (start-router!)
  (mount-root))
