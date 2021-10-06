(ns service.frontend.api.core
  #?(:cljs (:require-macros [service.frontend.api.core]))
  (:require [clojure.string]
            #?(:cljs [re-frame.core :as rf])
            #?(:cljs [service.frontend.config :as config])
            [utilities.core :refer [map-keys map-vals]]))

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
          (let [*uri* 'uri]
            `(merge {:method ~request-method
                     :uri (str (utilities.core/remove-trailing-slash ~*uri*) ~uri)
                     :response-format (ajax.edn/edn-response-format)}
                    (when ~body
                      {:body (str ~body)
                       :headers {"Content-Type" "application/edn; charset=utf-8"}})
                    (when ~query-params
                      {:url-params (service.frontend.api.core/normalize-query-params ~query-params)
                       :vec-strategy :java})))))

#?(:clj (defmacro def-request-event [event-kw args request-map]
          `(re-frame.core/reg-event-fx ~event-kw
             [(re-frame.core/inject-cofx (keyword (namespace ~event-kw) "uri"))
              (re-frame.core/inject-cofx (keyword (namespace ~event-kw) "tokens-path"))]
             (fn [{:keys [~'db ~'uri ~'tokens-path]} [~'_ ~'on-success ~'on-failure ~@args]]
               {:dispatch [::with-relogin {:on-success ~'on-success
                                           :on-failure ~'on-failure
                                           :retry-count 3
                                           :login-event-kw (keyword (namespace ~event-kw) "refresh-tokens")
                                           :tokens-path-vec (if (vector? ~'tokens-path)
                                                              ~'tokens-path
                                                              [~'tokens-path])
                                           :request-map ~request-map}]}))))

#?(:cljs (rf/reg-event-fx ::with-relogin
           (fn [{:keys [db]} [_ {:keys [on-success on-failure
                                        retry-count login-event-kw
                                        tokens-path-vec request-map]} new-tokens]]
             (let [tokens (when new-tokens
                            new-tokens
                            (get-in db tokens-path-vec))
                   _ (when config/debug? (.log js/console ["API call... "
                                                           {:new-tokens new-tokens}]))
                   props {:on-success on-success
                          :on-failure on-failure
                          :retry-count retry-count
                          :login-event-kw login-event-kw
                          :tokens-path-vec tokens-path-vec
                          :request-map request-map}]
               (merge (when new-tokens
                        {:db (assoc-in db tokens-path-vec new-tokens)})
                      {:fx [[:http-xhrio (-> request-map
                                             (merge
                                              {:on-success on-success
                                               :on-failure [::relogin props]})
                                             (assoc-in [:headers "Authorization"]
                                                       (str "Bearer " (:access-token tokens))))]]})))))

#?(:cljs (rf/reg-event-fx ::relogin
           (fn [{:keys [db]} [_ {:keys [on-success on-failure
                                        retry-count login-event-kw
                                        tokens-path-vec request-map
                                        request-response]} response]]
             (let [[login-response request-response] (if (nil? request-response)
                                                       [nil response]
                                                       [response request-response])]
               (if (and (pos? retry-count)
                        (= 401 (:status request-response)))
                 (let [_ (when config/debug? (.log js/console ["Relogin... "
                                                               {:login-response login-response}]))
                       props {:on-success on-success
                              :on-failure on-failure
                              :retry-count (dec retry-count)
                              :login-event-kw login-event-kw
                              :tokens-path-vec tokens-path-vec
                              :request-map request-map}]
                   {:dispatch [login-event-kw
                               [::with-relogin props]
                               [::relogin (assoc props :request-response request-response)]
                               (get-in db (conj tokens-path-vec :refresh-token))]})
                 {:dispatch (conj on-failure request-response)})))))
