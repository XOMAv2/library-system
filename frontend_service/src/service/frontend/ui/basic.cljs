(ns service.frontend.ui.basic
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [utilities.core :refer [class-concat any-or-coll->coll remove-nth dissoc-in]]
            [service.frontend.subs :as subs]
            [service.frontend.forms :as forms]
            [service.frontend.icons.outline :as icons]
            [service.frontend.events :as-alias events]
            [service.frontend.ui.styles :as styles]))

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
  [:button {:class styles/button-style}
   [:div.flex.flex-row.items-center
    [:span.mr-2 text]
    [three-dot-loader {:class "bg-white"}]]])

(defn- input-value [e]
  (-> e .-target .-value))

(defn- input-number-value [e]
  (-> e .-target .-value js/Number. .valueOf))

(defn- input-checked [e]
  (-> e .-target .-checked))

(defn input [{:keys [label class type form-path field-path] :as props}]
  (let [props (dissoc props :label :class :type :form-path :field-path)
        value @(rf/subscribe [::forms/get-field-value form-path field-path])
        errors @(rf/subscribe [::forms/get-field-errors form-path field-path])
        submitted? @(rf/subscribe [::forms/get-form-submitted? form-path])
        disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])]
    [:div.space-y-2
     [:label.font-medium label
      [:input (merge props
                     {:class (class-concat styles/input-style (when label "mt-2") class)
                      :disabled (when disabled? true)
                      :type (case type
                              "uuid" "text"
                              type)
                      :value value
                      :on-change #(rf/dispatch [::forms/set-field-value
                                                form-path field-path
                                                (case type
                                                  "number" (input-number-value %)
                                                  "uuid" (uuid (input-number-value %))
                                                  (input-value %))])})]]
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
            [:button {:class (class-concat styles/icon-button-style
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
         [:button {:class (class-concat styles/outline-button-style "w-full")
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

(defn select [{:keys [label class form-path field-path key-name-map default-key]
               :or {default-key nil}}]
  (reagent/create-class
   {:component-did-mount
    (fn [_]
      (rf/dispatch [::forms/set-field-value form-path field-path default-key]))

    :reagent-render
    (fn [{:keys [label class form-path field-path key-name-map default-key]
          :or {default-key nil}}]
      (let [value @(rf/subscribe [::forms/get-field-value form-path field-path])
            errors @(rf/subscribe [::forms/get-field-errors form-path field-path])
            submitted? @(rf/subscribe [::forms/get-form-submitted? form-path])
            disabled? @(rf/subscribe [::forms/get-form-disabled? form-path])
            default-key-name (get key-name-map default-key "")
            index-key-name (->> (dissoc key-name-map default-key)
                                (map-indexed (fn [i [k n]] [(str i) k n]))
                                (#(conj % ["-1" default-key default-key-name]))
                                (vec))
            index-key (->> (map (fn [[i k _]] [i k]) index-key-name)
                           (into {}))
            key-index (->> (map (fn [[i k _]] [k i]) index-key-name)
                           (into {}))]
        [:div.space-y-2
         [:label.font-medium label
          [:select {:class (class-concat styles/outline-button-style "block w-full"
                                         (when label "mt-2") class)
                    :disabled (when disabled? true)
                    :value (get key-index value)
                    :on-change #(rf/dispatch [::forms/set-field-value
                                              form-path field-path
                                              (->> (input-value %)
                                                   (get index-key))])}
           (for [[index key name] index-key-name]
             ^{:key [form-path field-path index key name]}
             [:option {:value index} name])]]

         (when submitted?
           (for [e (when (coll? errors) errors)]
             ^{:key [form-path field-path e]}
             [:p.text-red-500.text-sm.font-medium e]))]))}))

(defn form
  [{:keys [form-path form-value title submit-name event-ctor
           footer explainer disabled? on-submit]} & inputs]
  (reagent/create-class
   {:component-did-mount
    (fn [_]
      (let [select-defaults (->> inputs
                                 (filter (fn [[component props]] (= select component)))
                                 (map (fn [[_ props]]
                                        (when (= form-path (:form-path props))
                                          [(:field-path props) (:default-key props)])))
                                 (into {}))
            form-value (merge select-defaults form-value)]
        (rf/dispatch [::forms/set-form-value form-path form-value]))
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
         [:div {:class styles/card-style}

          [:form.space-y-4 {:on-change #(rf/dispatch [::forms/explain-form form-path])
                            :on-submit #(.preventDefault %)}
           (when title
             [:h2.text-center.text-3xl.font-extrabold.text-gray-900
              title])
           #_"TODO: form body overflow scroll"
           #_[:div.space-y-4.overflow-y-auto.p-1.max-h-full #_{:class "max-h-[35rem]"}
              inputs]
           (for [input inputs]
             ^{:key [form-path input]}
             [:div input])
           (when (or submit-name footer)
             [:div.flex.justify-between.items-center
              (when submit-name
                [:button {:class styles/button-style
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