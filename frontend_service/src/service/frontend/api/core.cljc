(ns service.frontend.api.core
  #?(:cljs (:require-macros [service.frontend.api.core]))
  (:require [clojure.string]
            [ajax.edn]
            [utilities.core :refer [map-keys map-vals
                                    remove-trailing-slash]]))

(defn normalize-query-params
  "Checks that map keys are keywords or non-blank strings.
   Replaces nil values and nil values in top-level collections with empty strings for
   http-kit-like serialization."
  [query-params-map]
  (map-keys (fn [k]
              (when-not (or (keyword? k)
                            (and (string? k) (clojure.string/blank? "")))
                (throw (#?(:clj Exception. :cljs js/Error.) "Malformed query params map."))))
            query-params-map)
  (map-vals (fn [v]
              (cond
                (nil? v) ""
                (coll? v) (map #(if (nil? %) "" %) v)
                :else v))
            query-params-map))

#_"TODO: check what will happen if post request body is empty here and in http-kit."

#?(:clj (defmacro make-request
          [request-method uri & [body query-params]]
          `(let [~'tokens-path (if (vector? ~'tokens-path)
                                 (conj ~'tokens-path :access-token)
                                 [~'tokens-path :access-token])]
             (merge {:method ~request-method
                     :uri (str (utilities.core/remove-trailing-slash ~'uri) ~uri)
                     :response-format (ajax.edn/edn-response-format)
                     :headers (merge {"Authorization" (str "Bearer " (get-in ~'db ~'tokens-path))}
                                     (when ~body
                                       {"Content-Type" "application/edn; charset=utf-8"}))
                     :on-success ~'success
                     :on-failure ~'failure}
                    (when ~body
                      {:body (str ~body)})
                    (when ~query-params
                      {:url-params (service.frontend.api.core/normalize-query-params ~query-params)
                       :vec-strategy :java})))))

#?(:clj (defmacro def-request-event [event-kw args request-map]
          `(re-frame.core/reg-event-fx ~event-kw
             [(re-frame.core/inject-cofx (keyword (namespace ~event-kw) "uri"))
              (re-frame.core/inject-cofx (keyword (namespace ~event-kw) "tokens-path"))]
             (fn [{:keys [~'db ~'uri ~'tokens-path]} [~'_ ~'success ~'failure ~@args]]
               {:http-xhrio ~request-map}))))
