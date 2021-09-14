(ns utilities.api.core
  (:require [org.httpkit.client :as http]
            [utilities.core :refer [parse-content-type when-let*]]
            [utilities.muuntaja :refer [muuntaja-instance]]
            [clojure.string]
            [muuntaja.core :as muuntaja]))

(defn sync-request
  "Отправляет синхронный http-запрос.
   Возвращает статус и тело ответа.
   В случае отсутствия ответа возвращает статус 503 и сообщение об ошибке в теле
   В случае невозможности декодирования тела ответа вместо него
   будет помещён nil, а статус будет сохранён."
  [options]
  (let [response (http/request options)
        {:keys [error status body headers]} @response]
    (if error
      {:status 503
       :body {:message "Service Unavailable."}}

      {:status status
       :body (when-let* [{:keys [content-type]} headers
                         _ (-> content-type clojure.string/blank? not)
                         content-type (parse-content-type content-type)]
               (try (muuntaja/decode muuntaja-instance content-type body)
                    (catch Exception _ nil)))})))
