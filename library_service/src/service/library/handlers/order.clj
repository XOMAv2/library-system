(ns service.library.handlers.order
  (:require [service.library.tables.order :as o-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.time :as time]))

(defn add-order
  [{{order                :body}   :parameters
    {{order-table :order} :tables} :db
    {service-uri          :order}  :services-uri}]
  (let [order (merge {:booking-date (time/now)
                      :receiving-date nil
                      :return-date nil
                      :condition nil}
                     order)]
    (try (let [order (o-ops/-add order-table order)]
           {:status 201
            :body order
            :headers {"Location" (str (remove-trailing-slash service-uri)
                                      "/api/orders/"
                                      (:uid order))}})
         (catch Exception e
           {:status 400
            :body {:type (-> e type str)
                   :message (ex-message e)}}))))

(defn get-order
  [{{{:keys [uid]}        :path}   :parameters
    {{order-table :order} :tables} :db}]
  (if-let [order (o-ops/-get order-table uid)]
    {:status 200
     :body order}
    {:status 404
     :body {:message (str "Order with uid `" uid "` is not found.")}}))

(defn get-all-orders
  [{{order-query          :query}  :parameters
    {{order-table :order} :tables} :db}]
  (let [orders (if (not-empty order-query)
                 (o-ops/-get-all-by-keys order-table order-query)
                 (o-ops/-get-all order-table))]
    {:status 200
     :body {:orders orders}}))

(defn update-order
  [{{{:keys [uid]}        :path
     order                :body}   :parameters
    {{order-table :order} :tables} :db}]
  (try (if-let [order (o-ops/-update order-table uid order)]
         {:status 200
          :body order}
         {:status 404
          :body {:message (str "Order with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn update-all-orders
  [{{order-query          :query
     order                :body}   :parameters
    {{order-table :order} :tables} :db}]
  (try (let [order (o-ops/-update-all-by-keys order-table order-query order)]
         {:status 200
          :body order})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-order
  [{{{:keys [uid]}        :path}   :parameters
    {{order-table :order} :tables} :db}]
  (if-let [order (o-ops/-delete order-table uid)]
    {:status 200
     :body order}
    {:status 404
     :body {:message (str "Order with uid `" uid "` is not found.")}}))
