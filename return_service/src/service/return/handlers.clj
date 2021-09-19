(ns service.return.handlers
  (:require [service.return.tables.user-limit :as ul-ops]
            [utilities.core :refer [remove-trailing-slash]]))

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
