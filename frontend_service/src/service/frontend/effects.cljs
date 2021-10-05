(ns service.frontend.effects
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy]))

(rf/reg-fx ::navigate
  (fn [{:keys [route route-params query-params replace?]}]
    (if replace?
      (reitit.frontend.easy/replace-state route route-params query-params)
      (reitit.frontend.easy/push-state route route-params query-params))))

(rf/reg-fx ::console-log
  (fn [& forms]
    (apply js/console.log forms)))

(rf/reg-fx ::show-alert
  (fn [msg]
    (js/alert msg)))
