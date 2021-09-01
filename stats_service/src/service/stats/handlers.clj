(ns service.stats.handlers
  (:require [service.stats.tables.stat-record :as srops]
            [utilities.tables.client :as cops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]
            [utilities.time :as time]))

(defn add-stat-record
  [{{stat-record                      :body}    :parameters
    {{stat-record-table :stat-record} :tables}  :db
    {service-uri                      :session} :services-uri}]
  (let [stat-record (assoc stat-record :receive-time (time/now))]
    (try (let [stat-record (srops/-add stat-record-table stat-record)]
           {:status 201
            :body stat-record
            :headers {"Location" (str (remove-trailing-slash service-uri)
                                      "/api/stats/"
                                      (:uid stat-record))}})
         (catch Exception e
           {:status 400
            :body {:type (type e)
                   :message (ex-message e)}}))))

(defn get-stat-record
  [{{{:keys [uid]}                    :path}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (if-let [stat-record (srops/-get stat-record-table uid)]
    {:status 200
     :body stat-record}
    {:status 404
     :body {:message (str "Statistical record with uid `" uid "` is not found.")}}))

(defn get-all-stat-records
  [{{{stat-record-table :stat-record} :tables} :db}]
  (let [stat-records (srops/-get-all stat-record-table)]
    {:status 200
     :body {:stats stat-records}}))

(defn get-all-stat-records-by-service
  [{{{:keys [service]}                :path}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (let [stat-records (srops/-get-all-by-service stat-record-table service)]
    {:status 200
     :body {:stats stat-records}}))

(defn update-stat-record
  [{{{:keys [uid]}                    :path
     stat-record                      :body}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (try (if-let [stat-record (srops/-update stat-record-table uid stat-record)]
         {:status 200
          :body stat-record}
         {:status 404
          :body {:message (str "Statistical record with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (type e)
                 :message (ex-message e)}})))

(defn delete-stat-record
  [{{{:keys [uid]}                    :path}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (if-let [stat-record (srops/-delete stat-record-table uid)]
    {:status 200
     :body stat-record}
    {:status 404
     :body {:message (str "Statistical record with uid `" uid "` is not found.")}}))

(defn delete-all-stat-records
  [{{{stat-record-table :stat-record} :tables} :db}]
  (if-let [stat-records (srops/-delete-all stat-record-table)]
    {:status 200
     :body {:stats stat-records}}))

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
