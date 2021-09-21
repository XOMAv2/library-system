(ns service.book.handlers
  (:require [service.book.tables.book :as b-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.library :as librari-api]
            [better-cond.core :as b]))

(defn add-book
  [{{book               :body}    :parameters
    {{book-table :book} :tables}  :db
    {service-uri        :book} :services-uri}]
  (try (let [book (b-ops/-add book-table book)]
         {:status 201
          :body book
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/books/"
                                    (:uid book))}})
       (catch Exception e
         {:status 400
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
         {:status 400
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

    :let [library-resp (-> library-service
                           (librari-api/-delete-all-library-books {:book-uid uid})
                           :status)]

    (#{500 503} library-resp)
    (do (b-ops/-restore book-table uid)
        {:status 502
         :body {:message "Error during the library service call."}})

    (not= 200 library-resp)
    (do (b-ops/-restore book-table uid)
        {:status 500
         :body {:message "Invalid book service credentials."}})

    :else
    {:status 200
     :body book}))

(defn restore-book
  [{{{:keys [uid]}      :path}    :parameters
    {{book-table :book} :tables}  :db
    {library-service    :library} :services}]
  (b/cond
    :let [book (b-ops/-restore book-table uid)]

    (nil? book)
    {:status 404
     :body {:message (str "Book with uid `" uid "` is not found.")}}

    :let [library-resp (-> library-service
                           (librari-api/-restore-all-library-books {:book-uid uid})
                           :status)]

    (#{500 503} library-resp)
    (do (b-ops/-delete book-table uid)
        {:status 502
         :body {:message "Error during the library service call."}})

    (not= 200 library-resp)
    (do (b-ops/-delete book-table uid)
        {:status 500
         :body {:message "Invalid book service credentials."}})

    :else
    {:status 200
     :body book}))
