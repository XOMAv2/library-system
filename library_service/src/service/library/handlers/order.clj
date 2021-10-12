(ns service.library.handlers.order
  (:require [service.library.tables.order :as o-ops]
            [service.library.tables.library :as l-ops]
            [service.library.tables.library-book :as lb-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.time :as time]
            [utilities.api.book :as book-api]
            [utilities.api.rating :as rating-api]
            [utilities.api.return :as return-api]
            [better-cond.core :as b]
            [clojure.core.match :refer [match]]))

#_"TODO: check library books when processing orders."

(defn add-order
  [{{order                              :body}    :parameters
    {{order-table        :order
      library-book-table :library-book} :tables}  :db
    {service-uri                        :order}   :services-uri
    {return-service                     :return} :services}]
  (b/cond
    :let [order (merge {:booking-date (time/now)
                        :receiving-date nil
                        :return-date nil
                        :condition nil}
                       order)
          {:keys [user-uid library-uid book-uid]} order]

    :let [lb-prev (->> {:library-uid library-uid
                        :book-uid book-uid}
                       (lb-ops/-get-all-by-keys library-book-table)
                       first)]

    (nil? lb-prev)
    {:status 404
     :body {:message (str "Book with uid `" (:book-uid order) "` in"
                          "library with uid `" (:library-uid order) "` is not found.")}}

    (not (and (:is-available lb-prev)
              (> (:total-quantity lb-prev) (:granted-quantity lb-prev))))
    {:status 409
     :body {:message "Can't create order due to book unavailability."}}

    :let [library-book (try (-> library-book-table
                                (lb-ops/-update (:uid lb-prev)
                                                {:granted-quantity (inc (:granted-quantity lb-prev))}))
                            (catch Exception e e))]

    (instance? Exception library-book)
    (let [e library-book]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})

    (nil? library-book)
    {:status 404
     :body {:message (str "Library book with uid `" (:uid lb-prev) "` is not found.")}}

    :let [return-resp (return-api/-update-available-limit-by-user-uid return-service user-uid -1)]

    (not= 200 (:status return-resp))
    (do (when (nil? (try (lb-ops/-update library-book-table
                                         (:uid lb-prev)
                                         {:granted-quantity (:granted-quantity lb-prev)})
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          404           {:status 404
                         :body (:body return-resp)}
          422           {:status 422
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :let [order (try (o-ops/-add order-table order)
                     (catch Exception e e))]

    (instance? Exception order)
    (do (when (nil? (try (lb-ops/-update library-book-table
                                         (:uid lb-prev)
                                         {:granted-quantity (:granted-quantity lb-prev)})
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (not= 200 (-> return-service
                            (return-api/-update-available-limit-by-user-uid user-uid 1)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (let [e order]
          {:status 422
           :body {:type (-> e type str)
                  :message (ex-message e)}}))

    :else
    {:status 201
     :body order
     :headers {"Location" (str (remove-trailing-slash service-uri)
                               "/api/orders/"
                               (:uid order))}}))

(defn get-order
  [{{{:keys [uid]}            :path}    :parameters
    {{order-table   :order
      library-table :library} :tables}  :db
    {book-service             :book} :services}]
  (b/cond
    :let [order (o-ops/-get order-table uid)]

    (nil? order)
    {:status 404
     :body {:message (str "Order with uid `" uid "` is not found.")}}

    :else
    (let [book (book-api/-get-book book-service (:book-uid order))
          book (when (= 200 (:status book))
                 {:book (:body book)})
          library (l-ops/-get library-table (:library-uid order))
          library (when (some? library)
                    {:library library})]
      {:status 200
       :body (merge order book library)})))

(defn get-all-orders
  [{{order-query          :query}  :parameters
    {{order-table :order} :tables} :db}]
  (let [orders (if (not-empty order-query)
                 (o-ops/-get-all-by-keys order-table order-query)
                 (o-ops/-get-all order-table))]
    {:status 200
     :body {:orders orders}}))

(defn update-order
  "Only the first setting of the condition field value will affect the user rating."
  [{{{:keys [uid]}                      :path
     order                              :body}    :parameters
    {{order-table        :order
      library-book-table :library-book} :tables}  :db
    {return-service                     :return
     rating-service                     :rating} :services}]
  (b/cond
    :let [prev-order (o-ops/-get order-table uid)
          {prev-return-date :return-date
           prev-condition :condition} prev-order
          {:keys [return-date condition user-uid]} order]

    (nil? prev-order)
    {:status 404
     :body {:message (str "Order with uid `" uid "` is not found.")}}

    :let [returning? (and (nil? prev-return-date) return-date)
          return-resp (if returning?
                        (return-api/-update-available-limit-by-user-uid return-service user-uid 1)
                        {:status 200})]

    (not= 200 (:status return-resp))
    (match (:status return-resp)
      (:or 500 503) {:status 502
                     :body {:message "Error during the return service call."
                            :response return-resp}}
      (:or 401 403) {:status 500
                     :body {:message "Unable to access the return service due to invalid credentials."
                            :response return-resp}}
      404           {:status 404
                     :body (:body return-resp)}
      422           {:status 422
                     :body (:body return-resp)}
      :else         {:status 500
                     :body {:message "Error during the return service call."
                            :response return-resp}})

    :let [library-book (if returning?
                         (try (lb-ops/-update-granted-quantity-by-book-uid-and-library-uid
                               library-book-table
                               (select-keys prev-order [:book-uid :library-uid])
                               -1)
                              (catch Exception e e))
                         {:status 200})]

    (nil? library-book)
    (do (when (not= 200 (-> return-service
                            (return-api/-update-available-limit-by-user-uid user-uid -1)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        {:status 404
         :body {:message (str "Book with uid `" (:book-uid prev-order) "` in"
                              "library with uid `" (:library-uid prev-order) "` is not found.")}})

    (instance? Exception library-book)
    (do (when (not= 200 (-> return-service
                            (return-api/-update-available-limit-by-user-uid user-uid -1)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (let [e library-book]
          {:status 422
           :body {:type (-> e type str)
                  :message (ex-message e)}}))

    :let [order (try (o-ops/-update order-table uid order)
                     (catch Exception e e))]

    (instance? Exception order)
    (do (when (nil? (try (lb-ops/-update-granted-quantity-by-book-uid-and-library-uid
                          library-book-table
                          (select-keys prev-order [:book-uid :library-uid])
                          1)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (not= 200 (-> return-service
                            (return-api/-update-available-limit-by-user-uid user-uid -1)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (let [e order]
          {:status 422
           :body {:type (-> e type str)
                  :message (ex-message e)}}))

    (nil? order)
    (do (when (nil? (try (lb-ops/-update-granted-quantity-by-book-uid-and-library-uid
                          library-book-table
                          (select-keys prev-order [:book-uid :library-uid])
                          1)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (not= 200 (-> return-service
                            (return-api/-update-available-limit-by-user-uid user-uid -1)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        {:status 404
         :body {:message (str "Order with uid `" uid "` is not found.")}})

    :let [rating-resp (if (and (nil? prev-condition) condition)
                        (rating-api/-update-rating-by-user-uid rating-service user-uid condition)
                        {:status 200})]

    (not= 200 (:status rating-resp))
    (do (when (nil? (try (lb-ops/-update-granted-quantity-by-book-uid-and-library-uid
                          library-book-table
                          (select-keys prev-order [:book-uid :library-uid])
                          1)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (not= 200 (-> return-service
                            (return-api/-update-available-limit-by-user-uid user-uid -1)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (nil? (try (o-ops/-update order-table uid prev-order)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status rating-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the rating service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the rating service due to invalid credentials."
                                :response return-resp}}
          404           {:status 404
                         :body (:body return-resp)}
          422           {:status 422
                         :body (:body rating-resp)}
          :else         {:status 500
                         :body {:message "Error during the rating service call."
                                :response return-resp}}))
    
    :else
    {:status 200
     :body order}))

(defn update-all-orders
  "Condition field update will not affect the user rating."
  [{{order-query                        :query
     order                              :body}   :parameters
    {{order-table        :order
      library-book-table :library-book} :tables} :db}]
  (try (let [orders (o-ops/-update-all-by-keys order-table order-query order)]
         {:status 200
          :body {:orders orders}})
       (catch Exception e
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-order
  [{{{:keys [uid]}                      :path}   :parameters
    {{order-table        :order
      library-book-table :library-book} :tables} :db}]
  (b/cond
    :let [prev-order (o-ops/-get order-table uid)]

    (nil? prev-order)
    {:status 404
     :body {:message (str "Order with uid `" uid "` is not found.")}}

    :let [library-book (try (lb-ops/-update-granted-quantity-by-book-uid-and-library-uid
                             library-book-table
                             (select-keys prev-order [:book-uid :library-book])
                             -1)
                            (catch Exception e e))]

    (instance? Exception library-book)
    (let [e library-book]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})

    :let [order (o-ops/-delete order-table uid)]

    (nil? order)
    (do (when (and library-book
                   (instance? Exception
                              (try (lb-ops/-update-granted-quantity-by-book-uid-and-library-uid
                                    library-book-table
                                    (select-keys prev-order [:book-uid :library-book])
                                    1)
                                   (catch Exception e e))))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        {:status 404
         :body {:message (str "Order with uid `" uid "` is not found.")}})

    :else
    {:status 200
     :body order}))
