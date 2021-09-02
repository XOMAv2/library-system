(ns service.book.handlers
  (:require [service.book.tables.book :as bops]
            [utilities.tables.client :as cops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]
            [utilities.time :as time]))

(defn add-book
  [{{book               :body}    :parameters
    {{book-table :book} :tables}  :db
    {service-uri        :book} :services-uri}]
  (try (let [book (bops/-add book-table book)]
         {:status 201
          :body book
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/books/"
                                    (:uid book))}})
       (catch Exception e
         {:status 400
          :body {:type (-> type str)
                 :message (ex-message e)}})))

(defn get-book
  [{{{:keys [uid]}      :path}   :parameters
    {{book-table :book} :tables} :db}]
  (if-let [book (bops/-get book-table uid)]
    {:status 200
     :body book}
    {:status 404
     :body {:message (str "Book with uid `" uid "` is not found.")}}))

(defn get-all-books
  [{{book               :query}  :parameters
    {{book-table :book} :tables} :db}]
  (let [books (if (not-empty book)
                (bops/-get-all-by-keys book-table book)
                (bops/-get-all book-table))]
    {:status 200
     :body {:books books}}))

(defn update-book
  [{{{:keys [uid]}      :path
     book               :body}   :parameters
    {{book-table :book} :tables} :db}]
  (try (if-let [book (bops/-update book-table uid book)]
         {:status 200
          :body book}
         {:status 404
          :body {:message (str "Book with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> type str)
                 :message (ex-message e)}})))

(defn delete-book
  [{{{:keys [uid]}      :path}   :parameters
    {{book-table :book} :tables} :db}]
  (if-let [book (bops/-delete book-table uid)]
    {:status 200
     :body book}
    {:status 404
     :body {:message (str "Book with uid `" uid "` is not found.")}}))

(defn get-token
  [{{{:keys [client-id client-secret]} :body}   :parameters
    {{client-table :client}            :tables} :db}]
  (if-let [client (cops/-get-by-client-id client-table client-id)]
    (if (:valid (hashers/verify client-secret
                                (:client-secret client)
                                {:limit #{:bcrypt+sha512}}))
      {:status 200
       :body {:token (auth/sign-jwt-refresh (select-keys client [:uid :role]))}}
      {:status 401
       :body {:message "Incorrect client secret."}})
    {:status 404
     :body {:message (str "Client with id `" client-id "` is not found.")}}))

(defn refresh-token
  [{{:keys [uid]}                    :identity
    {{client-table :client} :tables} :db}]
  (if-let [client (cops/-get client-table uid)]
    {:status 200
     :body {:token (auth/sign-jwt-refresh (select-keys client [:uid :role]))}}
    {:status 404
     :body {:message "Token credentials can't be found in the database."}}))

(defn verify-token
  []
  {:status 200
   :body ""
   :headers {}})
