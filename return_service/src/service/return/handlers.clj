(ns service.return.handlers
  (:require [service.return.tables.user-limit :as ul-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.session :as session-api]
            [better-cond.core :as b]
            [clojure.core.match :refer [match]]))

(defn add-user-limit
  [{{user-limit                     :body}    :parameters
    {{user-limit-table :user-limit} :tables}  :db
    {service-uri                    :return}  :services-uri
    {session-service                :session} :services}]
  (b/cond
    :let [user-uid (:user-uid user-limit)
          session-resp (-> session-service
                           (session-api/-get-user user-uid)
                           :status)]

    (not= 200 session-resp)
    (match session-resp
      (:or 500 503) {:status 502
                     :body {:message "Error during the session service call."}}
      (:or 401 403) {:status 500
                     :body {:message "Unable to acces the session service due to invalid credentials."}}
      404           {:status 422
                     :body {:message (str "User with uid `" user-uid "` is not found.")}}
      :else         {:status 500
                     :body {:message "Error during the session service call."}})

    :let [user-limit (try (ul-ops/-add user-limit-table user-limit)
                          (catch Exception e e))]

    (instance? Exception user-limit)
    (let [e user]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})

    :else
    {:status 201
     :body user-limit
     :headers {"Location" (str (remove-trailing-slash service-uri)
                               "/api/limits/"
                               (:uid user-limit))}}))

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
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn update-total-limit
  "Changing the total limit leads to a change in the available limit."
  [{{{:keys [user-uid delta]}       :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (try (if-let [user-limit (ul-ops/-update-total-limit-by-user-uid
                            user-limit-table user-uid delta)]
         {:status 200
          :body user-limit}
         {:status 404
          :body {:message (str "User limit with user uid `" user-uid "` is not found.")}})
       (catch Exception e
         {:status 422
          :body {:type (-> e type str)
                 :message (ex-message e)}})))

(defn update-available-limit
  "If the available limit is negative, the new value must be greater than the previous one."
  [{{{:keys [user-uid delta]}       :path}   :parameters
    {{user-limit-table :user-limit} :tables} :db}]
  (b/cond
    :let [user-limit (ul-ops/-get-by-user-uid user-uid)]

    (nil? user-limit)
    {:status 404
     :body (str "User limit with user uid `" user-uid "` is not found.")}

    :let [available-limit (:available-limit user-limit)
          new-available-limit (+ available-limit delta)]

    (and (neg? new-available-limit)
         (< new-available-limit available-limit))
    {:status 422
     :body {:message (str "The next value of the available limit must be at least " available-limit ".")}}

    :let [user-limit (try (-> user-limit-table
                              (ul-ops/-update-available-limit-by-user-uid user-uid delta))
                          (catch Exception e e))]

    (instance? Exception user-limit)
    (let [e user]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})
    
    (nil? user-limit)
    {:status 404
     :body (str "User limit with user uid `" user-uid "` is not found.")}
    
    :else
    {:status 200
     :body user-limit}))

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
