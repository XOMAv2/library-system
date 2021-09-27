(ns service.gateway.util)

(defmacro api-fn [[parameters-dtor & {:keys [headers-dtor]}]
                  [_ *-service :as form]]
  (let [service-sym (-> (name *-service)
                        (clojure.string/replace  #"-service$" "")
                        (symbol))]
    `(fn [{{~*-service ~(keyword service-sym)} :services
           ~(or parameters-dtor {}) :parameters
           ~(or headers-dtor {}) :headers}]
       (let [resp# ~form]
         (if (contains? #{401 403} (:status resp#))
           {:status 500
            :body {:message ~(str "Unable to access the " service-sym " service due to invalid credentials.")
                   :response resp#}}
           resp#)))))
