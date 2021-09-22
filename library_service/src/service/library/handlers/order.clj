(ns service.library.handlers.order
  (:require [service.library.tables.order :as o-ops]
            [service.library.tables.library :as l-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.time :as time]
            [utilities.api.book :as book-api]
            [utilities.api.session :as session-api]
            [better-cond.core :as b]
            [clojure.core.match :refer [match]]))

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
  [{{{:keys [uid]}            :path}    :parameters
    {{order-table   :order
      library-table :library} :tables}  :db
    {book-service             :book
     session-service          :session} :services}]
  (b/cond
    :let [order (o-ops/-get order-table uid)]

    (nil? order)
    {:status 404
     :body {:message (str "Order with uid `" uid "` is not found.")}}

    :else
    (let [book (book-api/-get-book book-service (:book-uid order))
          book (when (= 200 (:status book))
                 {:book (:body book)})
          user (session-api/-get-user session-service (:user-uid order))
          user (when (= 200 (:status user))
                 {:user (:body user)})
          library (l-ops/-get library-table (:library-uid order))
          library (when (some? library)
                    {:library library})]
      {:status 200
       :body (merge order book user library)})))

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
