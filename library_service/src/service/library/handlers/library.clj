(ns service.library.handlers.library
  (:require [service.library.tables.library :as l-ops]
            [utilities.core :refer [remove-trailing-slash]]))

(defn add-library
  [{{library                  :body}     :parameters
    {{library-table :library} :tables}   :db
    {service-uri              :library}  :services-uri}]
  (try (let [library (l-ops/-add library-table library)]
         {:status 201
          :body library
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/libraries/"
                                    (:uid library))}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn get-library
  [{{{:keys [uid]}            :path}   :parameters
    {{library-table :library} :tables} :db}]
  (if-let [library (l-ops/-get library-table uid)]
    {:status 200
     :body library}
    {:status 404
     :body {:message (str "library with uid `" uid "` is not found.")}}))

(defn get-all-libraries
  [{{library                  :query}  :parameters
    {{library-table :library} :tables} :db}]
  (let [libraries (if (not-empty library)
                    (l-ops/-get-all-by-keys library-table library)
                    (l-ops/-get-all library-table))]
    {:status 200
     :body {:libraries libraries}}))

(defn update-library
  [{{{:keys [uid]}            :path
     library                  :body}   :parameters
    {{library-table :library} :tables} :db}]
  (try (if-let [library (l-ops/-update library-table uid library)]
         {:status 200
          :body library}
         {:status 404
          :body {:message (str "library with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-library
  [{{{:keys [uid]}            :path}   :parameters
    {{library-table :library} :tables} :db}]
  (if-let [library (l-ops/-delete library-table uid)]
    {:status 200
     :body library}
    {:status 404
     :body {:message (str "library with uid `" uid "` is not found.")}}))
