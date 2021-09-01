(ns service.rating.handlers
  (:require [service.rating.tables.user-rating :as urops]
            [utilities.tables.client :as cops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]))

(defn add-user-rating
  [{{user-rating                      :body}    :parameters
    {{user-rating-table :user-rating} :tables}  :db
    {service-uri                      :session} :services-uri}]
  (try (let [user-rating (urops/-add user-rating-table user-rating)]
         {:status 201
          :body user-rating
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/ratings/"
                                    (:uid user-rating))}})
       (catch Exception e
         {:status 400
          :body {:type (-> type str)
                 :message (ex-message e)}})))

(defn get-user-rating
  [{{{:keys [uid]}                    :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (if-let [user-rating (urops/-get user-rating-table uid)]
    {:status 200
     :body user-rating}
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}))

(defn get-user-rating-by-user-uid
  [{{{:keys [user-uid]}               :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (if-let [user-rating (urops/-get-by-user-uid user-rating-table user-uid)]
    {:status 200
     :body user-rating}
    {:status 404
     :body {:message (str "User rating with user uid `" user-uid "` is not found.")}}))

(defn get-all-user-ratings
  [{{{user-rating-table :user-rating} :tables} :db}]
  (let [user-ratings (urops/-get-all user-rating-table)]
    {:status 200
     :body {:ratings user-ratings}}))

(defn update-user-rating
  [{{{:keys [uid]}                    :path
     user-rating                      :body}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (try (if-let [user-rating (urops/-update user-rating-table uid user-rating)]
         {:status 200
          :body user-rating}
         {:status 404
          :body {:message (str "User rating with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> type str)
                 :message (ex-message e)}})))

(defn update-rating-by-user-uid
  [{{{:keys [user-uid delta]}         :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (try (if-let [user-rating (urops/-update-rating-by-user-uid
                            user-rating-table user-uid delta)]
         {:status 200
          :body user-rating}
         {:status 404
          :body {:message (str "User rating with user uid `" user-uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> type str)
                 :message (ex-message e)}})))

(defn delete-user-rating
  [{{{:keys [uid]}                    :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (if-let [user-rating (urops/-delete user-rating-table uid)]
    {:status 200
     :body user-rating}
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}))

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
       :body {:message "Incorrect client's secret."}})
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
