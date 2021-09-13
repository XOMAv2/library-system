(ns service.library.handlers
  (:require [utilities.tables.client :as c-ops]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]))

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
       :body {:message "Incorrect client secret."}})
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
