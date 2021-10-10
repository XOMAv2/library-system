(ns service.frontend.forms
  (:require [utilities.core :refer [any-or-coll->coll]]
            [malli.error]
            [re-frame.core :as rf]))

#_"=============== EVENTS ==============="

(rf/reg-event-fx ::set-form-value
  (fn [{:keys [db]} [_ form-path value]]
    (when form-path
      {:db (assoc-in db (conj (any-or-coll->coll form-path) :value) value)})))

(rf/reg-event-fx ::set-form-value-if-empty
  (fn [{:keys [db]} [_ form-path value]]
    (when form-path
      (let [path (conj (any-or-coll->coll form-path) :value)
            prev-value (get-in db path)]
        (when (empty? prev-value)
          {:db (assoc-in db path value)})))))

(rf/reg-event-fx ::set-field-value
  (fn [{:keys [db]} [_ form-path field-path value]]
    (when (and form-path field-path)
      {:db (assoc-in db (concat (any-or-coll->coll form-path)
                                [:value]
                                (any-or-coll->coll field-path)) value)})))

(rf/reg-event-fx ::update-field-value
  (fn [{:keys [db]} [_ form-path field-path f & args]]
    (when (and form-path field-path)
      (let [path (concat (any-or-coll->coll form-path)
                         [:value]
                         (any-or-coll->coll field-path))]
        {:db (apply update-in db path f args)}))))

(rf/reg-event-fx ::update-form-value
  (fn [{:keys [db]} [_ form-path f & args]]
    (when form-path
      (let [path (conj (any-or-coll->coll form-path) :value)]
        {:db (apply update-in db path f args)}))))

(rf/reg-event-fx ::set-form-explainer
  (fn [{:keys [db]} [_ form-path explainer]]
    (when form-path
      {:db (assoc-in db (conj (any-or-coll->coll form-path) :explainer) explainer)})))

(rf/reg-event-fx ::explain-form
  (fn [{:keys [db]} [_ form-path]]
    (when form-path
      (let [form-path (any-or-coll->coll form-path)
            explainer (get-in db (conj form-path :explainer))
            form-value (get-in db (conj form-path :value))
            form-errors (-> form-value explainer malli.error/humanize)]
        (.log js/console [form-path explainer form-value form-errors])
        (def kekolol [form-path explainer form-value form-errors])
        {:db (assoc-in db (conj form-path :errors) form-errors)}))))

(rf/reg-event-fx ::set-form-submitted?
  (fn [{:keys [db]} [_ form-path value]]
    (when form-path
      {:db (assoc-in db (conj (any-or-coll->coll form-path) :submitted?) value)})))

(rf/reg-event-fx ::set-form-disabled?
  (fn [{:keys [db]} [_ form-path value]]
    (when form-path
      {:db (assoc-in db (conj (any-or-coll->coll form-path) :disabled?) value)})))

(rf/reg-event-fx ::set-form-loading?
  (fn [{:keys [db]} [_ form-path value]]
    (when form-path
      (let [form-path (any-or-coll->coll form-path)
            db (if value
                 (assoc-in db (conj form-path :disabled?) value)
                 db)]
        {:db (assoc-in db (conj form-path :loading?) value)}))))

#_"=============== SUBS ==============="

(rf/reg-sub ::get-form-value
  (fn [db [_ form-path]]
    (when form-path
      (get-in db (conj (any-or-coll->coll form-path) :value)))))

(rf/reg-sub ::get-form-errors
  (fn [db [_ form-path]]
    (when form-path
      (get-in db (conj (any-or-coll->coll form-path) :errors)))))

(rf/reg-sub ::get-form-level-errors
  (fn [db [_ form-path]]
    (when form-path
      (get-in db (concat (any-or-coll->coll form-path)
                         [:errors :malli/error])))))

(rf/reg-sub ::get-field-value
  (fn [db [_ form-path field-path]]
    (when (and form-path field-path)
      (get-in db (concat (any-or-coll->coll form-path)
                         [:value]
                         (any-or-coll->coll field-path))))))

(rf/reg-sub ::get-field-errors
  (fn [db [_ form-path field-path]]
    (when (and form-path field-path)
      (get-in db (concat (any-or-coll->coll form-path)
                         [:errors]
                         (any-or-coll->coll field-path))))))

(rf/reg-sub ::get-form-submitted?
  (fn [db [_ form-path]]
    (when form-path
      (let [val (get-in db (conj (any-or-coll->coll form-path) :submitted?))]
        val))))

(rf/reg-sub ::get-form-disabled?
  (fn [db [_ form-path]]
    (when form-path
      (get-in db (conj (any-or-coll->coll form-path) :disabled?)))))

(rf/reg-sub ::get-form-loading?
  (fn [db [_ form-path]]
    (when form-path
      (get-in db (conj (any-or-coll->coll form-path) :loading?)))))

(rf/reg-sub ::get-form-valid?
  (fn [db [_ form-path]]
    (when form-path
      (-> (get-in db (conj (any-or-coll->coll form-path) :errors))
          (empty?)))))
