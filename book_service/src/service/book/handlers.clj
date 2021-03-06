(ns service.book.handlers
  (:require [service.book.tables.book :as b-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.library :as library-api]
            [better-cond.core :as b]
            [clojure.core.match :refer [match]]))

(defn add-book
  [{{book               :body}    :parameters
    {{book-table :book} :tables}  :db
    {service-uri        :book}    :services-uri}]
  (try (let [book (b-ops/-add book-table book)]
         {:status 201
          :body book
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/books/"
                                    (:uid book))}})
       (catch Exception e
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn get-book
  [{{{:keys [uid]}      :path}   :parameters
    {{book-table :book} :tables} :db}]
  (if-let [book (b-ops/-get book-table uid)]
    {:status 200
     :body book}
    {:status 404
     :body {:message (str "Book with uid `" uid "` is not found.")}}))

(defn get-all-books
  [{{book-query         :query}  :parameters
    {{book-table :book} :tables} :db}]
  (let [books (if (not-empty book-query)
                (b-ops/-get-all-by-keys book-table book-query)
                (b-ops/-get-all book-table))]
    {:status 200
     :body {:books books}}))

(defn update-book
  [{{{:keys [uid]}      :path
     book               :body}   :parameters
    {{book-table :book} :tables} :db}]
  (try (if-let [book (b-ops/-update book-table uid book)]
         {:status 200
          :body book}
         {:status 404
          :body {:message (str "Book with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-book
  [{{{:keys [uid]}      :path}    :parameters
    {{book-table :book} :tables}  :db
    {library-service    :library} :services}]
  (b/cond
    :let [book (b-ops/-delete book-table uid)]

    (nil? book)
    {:status 404
     :body {:message (str "Book with uid `" uid "` is not found.")}}

    :let [library-book-resp (library-api/-delete-all-library-books library-service
                                                                   {:book-uid uid})]

    (not= 200 (:status library-book-resp))
    (do (when (nil? (try (b-ops/-restore book-table uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status library-book-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the library service call."
                                :response library-book-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the library service due to invalid credentials."
                                :response library-book-resp}}
          :else         {:status 500
                         :body {:message "Error during the library service call."
                                :response library-book-resp}}))

    :let [order-resp (library-api/-update-all-orders library-service
                                                     {:book-uid uid} {:book-uid nil})]

    (not= 200 (:status order-resp))
    (do (when (nil? (try (b-ops/-restore book-table uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (not= 200 (-> library-service
                            (library-api/-restore-all-library-books {:book-uid uid})
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status order-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the library service call."
                                :response order-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the library service due to invalid credentials."
                                :response order-resp}}
          422           {:status 422
                         :body (:body order-resp)}
          :else         {:status 500
                         :body {:message "Error during the library service call."
                                :response order-resp}}))

    :else
    {:status 200
     :body book}))

(defn restore-book
  "Book restore doesn't restore relations within orders!"
  [{{{:keys [uid]}      :path}    :parameters
    {{book-table :book} :tables}  :db
    {library-service    :library} :services}]
  (b/cond
    :let [book (b-ops/-restore book-table uid)]

    (nil? book)
    {:status 404
     :body {:message (str "Book with uid `" uid "` is not found.")}}

    :let [library-resp (library-api/-restore-all-library-books library-service
                                                               {:book-uid uid})]

    (not= 200 (:status library-resp))
    (do (when (nil? (try (b-ops/-delete book-table uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status library-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the library service call."
                                :response library-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the library service due to invalid credentials."
                                :response library-resp}}
          :else         {:status 500
                         :body {:message "Error during the library service call."
                                :response library-resp}}))

    :else
    {:status 200
     :body book}))
