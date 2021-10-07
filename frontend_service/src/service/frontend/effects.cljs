(ns service.frontend.effects
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy]))

(rf/reg-fx ::navigate
  (fn [{:keys [route path-params query-params replace?]}]
    (if replace?
      (reitit.frontend.easy/replace-state route path-params query-params)
      (reitit.frontend.easy/push-state route path-params query-params))))

(rf/reg-fx ::console-log
  (fn [msg]
    (.log js/console msg)))

(rf/reg-fx ::show-alert
  (fn [msg]
    (js/alert msg)))

(rf/reg-fx ::local-storage
  (fn [[key value]]
    (.setItem js/localStorage (name key) (str value))))
