(ns utilities.api.core
  (:require [org.httpkit.client :as http]
            [utilities.core :refer [parse-content-type when-let*]]
            [utilities.muuntaja :refer [muuntaja-instance]]
            [clojure.string]
            [muuntaja.core :as muuntaja]
            [diehard.core]
            [diehard.circuit-breaker]))

(def ^:private service-unavailable
  {:status 503
   :body {:message "Service Unavailable."}})

(defn sync-request
  "Отправляет синхронный http-запрос.
   Возвращает мапу с ключами :status и :body.
   В случае ошибки значение :status будет равно 503, а :body будет содержать сообщение об ошибке.
   В случае невозможности декодирования тела ответа значение ключа :body будет равно nil."
  [req-options]
  (let [response (http/request req-options)
        {:keys [error status body headers]} @response]
    (if error
      service-unavailable
      {:status status
       :body (when-let* [{:keys [content-type]} headers
                         _ (-> content-type clojure.string/blank? not)
                         content-type (parse-content-type content-type)]
               (try (muuntaja/decode muuntaja-instance content-type body)
                    (catch Exception _ nil)))})))

(defmacro cb-sync-request [cb req-options]
  `(try (diehard.core/with-circuit-breaker ~cb
          (sync-request ~req-options))
        (catch net.jodah.failsafe.CircuitBreakerOpenException _#
          ~service-unavailable)))

(defn make-cb [options]
  (-> (dissoc options :fail-on :fail-if)
      (assoc :fail-when service-unavailable)
      (diehard.circuit-breaker/circuit-breaker)))

(comment
  
  "Цепь будет разомкнуто после того как 3 из 6 запросов завершатся неудачно.
   Через 10 секунд после открытия цепи она будет переведена в полузакрытое состояние.
   Если последующие запросы будут успешны, то цепь будет замкнута."
  
  (make-cb {#_"Ratio to turn into :open state and back to :closed from :half-open."
            :failure-threshold-ratio [3 6]
            #_"Delay to turn into :half-open state from :half-open."
            :delay-ms 10000})
  )
