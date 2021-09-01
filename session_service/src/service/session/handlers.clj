(ns service.session.handlers
  (:require [service.session.tables.user :as uops]
            [clojure.set :refer [rename-keys]]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]))

(defn add-user
  [{{user               :body}    :parameters
    {{user-table :user} :tables}  :db
    {service-uri        :session} :services-uri}]
  (let [user (update user :password #(hashers/derive % {:alg :bcrypt+sha512}))
        user (rename-keys user {:password :password-hash})]
    (try (let [user (uops/-add user-table user)]
           {:status 201
            :body user
            :headers {"Location" (str (remove-trailing-slash service-uri)
                                      "/api/users/"
                                      (:uid user))}})
         (catch Exception e
           {:status 400
            :body {:type (-> type str)
                   :message (ex-message e)}}))))

(defn get-user
  [{{{:keys [uid]}      :path}   :parameters
    {{user-table :user} :tables} :db}]
  (if-let [user (uops/-get user-table uid)]
    {:status 200
     :body user}
    {:status 404
     :body {:message (str "User with uid `" uid "` is not found.")}}))

(defn get-all-users
  [{{{user-table :user} :tables} :db}]
  (let [users (uops/-get-all user-table)]
    {:status 200
     :body {:users users}}))

(defn update-user
  [{{{:keys [uid]}      :path
     user               :body}   :parameters
    {{user-table :user} :tables} :db}]
  (let [user (if (contains? user :password)
               (-> user
                   (update :password #(hashers/derive % {:alg :bcrypt+sha512}))
                   (rename-keys {:password :password-hash}))
               user)]
    (try (if-let [user (uops/-update user-table uid user)]
           {:status 200
            :body user}
           {:status 404
            :body {:message (str "User with uid `" uid "` is not found.")}})
         (catch Exception e
           {:status 400
            :body {:type (-> type str)
                   :message (ex-message e)}}))))

(defn delete-user
  [{{{:keys [uid]}      :path}   :parameters
    {{user-table :user} :tables} :db}]
  (if-let [user (uops/-delete user-table uid)]
    {:status 200
     :body user}
    {:status 404
     :body {:message (str "User with uid `" uid "` is not found.")}}))

(defn get-tokens
  [{{{:keys [email password]} :body}   :parameters
    {{user-table :user}       :tables} :db}]
  (if-let [user (uops/-get-by-email user-table email)]
    (if (:valid (hashers/verify password
                                (:password-hash user)
                                {:limit #{:bcrypt+sha512}}))
      {:status 200
       :body {:access-token (auth/sign-jwt-access (select-keys user [:uid :role]))
              :refresh-token (auth/sign-jwt-refresh (select-keys user [:uid :role]))}}
      {:status 401
       :body {:message "Incorrect password."}})
    {:status 404
     :body {:message (str "User with email `" email "` is not found.")}}))

(defn refresh-tokens
  [{{{:keys [refresh-token]} :body}   :parameters
    {{user-table :user}      :tables} :db}]
  (try (let [{:keys [uid]} (auth/unsign-jwt refresh-token)]
         (if-let [user (uops/-get user-table uid)]
           {:status 200
            :body {:access-token (auth/sign-jwt-access (select-keys user [:uid :role]))
                   :refresh-token (auth/sign-jwt-refresh (select-keys user [:uid :role]))}}
           {:status 404
            :body {:message "Token credentials can't be found in the database."}}))
       (catch Exception _
         {:status 401
          :body {:message "Token seems corrupt or manipulated."}})))

(defn verify-token
  [{{{:keys [access-token]} :body} :parameters}]
  (try (auth/unsign-jwt access-token)
       {:status 200
        :body ""
        :headers {}}
       (catch Exception _
         {:status 401
          :body {:message "Token seems corrupt or manipulated."}})))
