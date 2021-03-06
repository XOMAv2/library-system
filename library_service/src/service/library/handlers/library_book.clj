(ns service.library.handlers.library-book
  (:require [service.library.tables.library-book :as lb-ops]
            [utilities.core :refer [remove-trailing-slash]]))

(defn add-library-book
  [{{library-book                       :body}         :parameters
    {{library-book-table :library-book} :tables}       :db
    {service-uri                        :library-book} :services-uri}]
  (try (let [library-book (lb-ops/-add library-book-table library-book)]
         {:status 201
          :body library-book
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/library-books/"
                                    (:uid library-book))}})
       (catch Exception e
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn get-library-book
  [{{{:keys [uid]}                      :path}   :parameters
    {{library-book-table :library-book} :tables} :db}]
  (if-let [library-book (lb-ops/-get library-book-table uid)]
    {:status 200
     :body library-book}
    {:status 404
     :body {:message (str "Library-book relation with uid `" uid "` is not found.")}}))

(defn get-all-library-books
  [{{library-book-query                 :query}  :parameters
    {{library-book-table :library-book} :tables} :db}]
  (let [library-books (if (not-empty library-book-query)
                        (lb-ops/-get-all-by-keys library-book-table library-book-query)
                        (lb-ops/-get-all library-book-table))]
    {:status 200
     :body {:library-books library-books}}))

(defn update-library-book
  [{{{:keys [uid]}                      :path
     library-book                       :body}   :parameters
    {{library-book-table :library-book} :tables} :db}]
  (try (if-let [library-book (lb-ops/-update library-book-table uid library-book)]
         {:status 200
          :body library-book}
         {:status 404
          :body {:message (str "Library-book relation with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-library-book
  [{{{:keys [uid]}                      :path}   :parameters
    {{library-book-table :library-book} :tables} :db}]
  (if-let [library-book (lb-ops/-delete library-book-table uid)]
    {:status 200
     :body library-book}
    {:status 404
     :body {:message (str "Library-book relation with uid `" uid "` is not found.")}}))

(defn delete-all-library-books
  [{{library-book-query                 :query}  :parameters
    {{library-book-table :library-book} :tables} :db}]
  (let [library-book (-> library-book-table
                         (lb-ops/-delete-all-by-keys (or library-book-query {})))]
    {:status 200
     :body {:library-books library-book}}))

(defn restore-library-book
  [{{{:keys [uid]}                      :path}   :parameters
    {{library-book-table :library-book} :tables} :db}]
  (if-let [library-book (lb-ops/-restore library-book-table uid)]
    {:status 200
     :body library-book}
    {:status 404
     :body {:message (str "Library-book relation with uid `" uid "` is not found.")}}))

(defn restore-all-library-books
  [{{library-book-query                 :query}  :parameters
    {{library-book-table :library-book} :tables} :db}]
  (let [library-book (-> library-book-table
                         (lb-ops/-restore-all-by-keys (or library-book-query {})))]
    {:status 200
     :body {:library-books library-book}}))
