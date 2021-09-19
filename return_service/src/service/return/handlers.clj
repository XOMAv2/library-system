(ns service.return.handlers
  (:require [service.return.tables.user-limit :as ul-ops]
            [utilities.db.tables.client :as c-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]))

(defn add-user-limit
  [{{user-limit                     :body}   :parameters
    {{user-limit-table :user-limit} :tables} :db
    {service-uri                    :return} :services-uri}]
  (try (let [user-limit (ul-ops/-add user-limit-table user-limit)]
         {:status 201
          :body user-limit
          :headers {"Location" (str (remove-trailing-slash service-uri)
                                    "/api/limits/"
                                    (:uid user-limit))}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn get-user-limit
  [{{{:keys [uid]}                  :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (if-let [user-limit (ul-ops/-get user-limit-table uid)]
    {:status 200
     :body user-limit}
    {:status 404
     :body {:message (str "User limit with uid `" uid "` is not found.")}}))

(defn get-user-limit-by-user-uid
  [{{{:keys [user-uid]}             :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (if-let [user-limit (ul-ops/-get-by-user-uid user-limit-table user-uid)]
    {:status 200
     :body user-limit}
    {:status 404
     :body {:message (str "User limit with user uid `" user-uid "` is not found.")}}))

(defn get-all-user-limits
  [{{{user-limit-table :user-limit} :tables} :db}]
  (let [user-limits (ul-ops/-get-all user-limit-table)]
    {:status 200
     :body {:limits user-limits}}))

(defn update-user-limit
  [{{{:keys [uid]}                  :path
     user-limit                     :body}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (try (if-let [user-limit (ul-ops/-update user-limit-table uid user-limit)]
         {:status 200
          :body user-limit}
         {:status 404
          :body {:message (str "User limit with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn update-total-limit
  [{{{:keys [user-uid delta]}       :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (try (if-let [user-limit (ul-ops/-update-total-limit-by-user-uid
                            user-limit-table user-uid delta)]
         {:status 200
          :body user-limit}
         {:status 404
          :body {:message (str "User limit with user uid `" user-uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn update-available-limit
  [{{{:keys [user-uid delta]}       :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (try (if-let [user-limit (ul-ops/-update-available-limit-by-user-uid
                            user-limit-table user-uid delta)]
         {:status 200
          :body user-limit}
         {:status 404
          :body {:message (str "User limit with user uid `" user-uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-user-limit
  [{{{:keys [uid]}                  :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (if-let [user-limit (ul-ops/-delete user-limit-table uid)]
    {:status 200
     :body user-limit}
    {:status 404
     :body {:message (str "User limit with uid `" uid "` is not found.")}}))

(defn delete-user-limit-by-user-uid
  [{{{:keys [user-uid]}             :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (if-let [user-limit (ul-ops/-delete-by-user-uid user-limit-table user-uid)]
    {:status 200
     :body user-limit}
    {:status 404
     :body {:message (str "User limit with user uid `" user-uid "` is not found.")}}))

(defn restore-user-limit
  [{{{:keys [uid]}                  :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (if-let [user-limit (ul-ops/-restore user-limit-table uid)]
    {:status 200
     :body user-limit}
    {:status 404
     :body {:message (str "User limit with uid `" uid "` is not found.")}}))

(defn restore-user-limit-by-user-uid
  [{{{:keys [user-uid]}             :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (if-let [user-limit (ul-ops/-restore-by-user-uid user-limit-table user-uid)]
    {:status 200
     :body user-limit}
    {:status 404
     :body {:message (str "User limit with user uid `" user-uid "` is not found.")}}))

(defn get-token
  [{{{:keys [client-id client-secret]} :body}   :parameters
    {{client-table :client}            :tables} :db}]
  (if-let [client (c-ops/-get-by-client-id client-table client-id)]
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
  (if-let [client (c-ops/-get client-table uid)]
    {:status 200
     :body {:token (auth/sign-jwt-refresh (select-keys client [:uid :role]))}}
    {:status 404
     :body {:message "Token credentials can't be found in the database."}}))

(defn verify-token
  [_]
  {:status 200
   :body ""
   :headers {}})
