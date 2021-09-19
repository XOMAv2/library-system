(ns service.stats.handlers
  (:require [service.stats.tables.stat-record :as sr-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.time :as time]))

(defn add-stat-record
  [{{stat-record                      :body}   :parameters
    {{stat-record-table :stat-record} :tables} :db
    {service-uri                      :stats}  :services-uri}]
  (let [stat-record (merge {:receive-time (time/now)
                            :content-type "text/plain"}
                           stat-record)]
    (try (let [stat-record (sr-ops/-add stat-record-table stat-record)]
           {:status 201
            :body stat-record
            :headers {"Location" (str (remove-trailing-slash service-uri)
                                      "/api/stats/"
                                      (:uid stat-record))}})
         (catch Exception e
           {:status 400
            :body {:type (-> e type str)
                   :message (ex-message e)}}))))

(defn get-stat-record
  [{{{:keys [uid]}                    :path}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (if-let [stat-record (sr-ops/-get stat-record-table uid)]
    {:status 200
     :body stat-record}
    {:status 404
     :body {:message (str "Statistical record with uid `" uid "` is not found.")}}))

(defn get-all-stat-records
  [{{{:keys [service]}                :query}  :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (let [stat-records (if service
                       (sr-ops/-get-all-by-service stat-record-table service)
                       (sr-ops/-get-all stat-record-table))]
    {:status 200
     :body {:stats stat-records}}))

(defn update-stat-record
  [{{{:keys [uid]}                    :path
     stat-record                      :body}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (try (if-let [stat-record (sr-ops/-update stat-record-table uid stat-record)]
         {:status 200
          :body stat-record}
         {:status 404
          :body {:message (str "Statistical record with uid `" uid "` is not found.")}})
       (catch Exception e
         {:status 400
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn delete-stat-record
  [{{{:keys [uid]}                    :path}   :parameters
    {{stat-record-table :stat-record} :tables} :db}]
  (if-let [stat-record (sr-ops/-delete stat-record-table uid)]
    {:status 200
     :body stat-record}
    {:status 404
     :body {:message (str "Statistical record with uid `" uid "` is not found.")}}))

(defn delete-all-stat-records
  [{{{stat-record-table :stat-record} :tables} :db}]
  (let [stat-records (sr-ops/-delete-all stat-record-table)]
    {:status 200
     :body {:stats stat-records}}))
