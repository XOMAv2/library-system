(ns service.session.handlers
  (:require [service.session.tables.user :as u-ops]
            [clojure.set :refer [rename-keys]]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.auth :as auth]
            [buddy.hashers :as hashers]
            [utilities.api.library :as library-api]
            [utilities.api.rating :as rating-api]
            [utilities.api.return :as return-api]
            [better-cond.core :as b]
            [clojure.core.match :refer [match]]))

(defn add-user
  [{{user               :body}    :parameters
    {{user-table :user} :tables}  :db
    {service-uri        :session} :services-uri
    {rating-service     :rating
     return-service     :return}  :services}]
  (b/cond
    :let [user (update user :password #(hashers/derive % {:alg :bcrypt+sha512}))
          user (rename-keys user {:password :password-hash})
          user (try (u-ops/-add user-table user)
                    (catch Exception e e))]

    (instance? Exception user)
    (let [e user]
      {:status 400
       :body {:type (-> e type str)
              :message (ex-message e)}})

    :let [user-uid (:uid user)]

    :let [rating {:user-uid user-uid
                  :rating 5}
          rating-resp (rating-api/-add-user-rating rating-service rating)
          rating (:body rating-resp)]

    (not= 201 (:status rating-resp))
    (do (u-ops/-delete user-table user-uid)
        (match (:status rating-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the rating service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the rating service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the rating service."}}
          :else         {:status 500
                         :body {:message "Error during the rating service call."}}))

    :let [limit {:user-uid user-uid
                 :total-limit 5
                 :available-limit 5}
          return-resp (return-api/-add-user-limit return-service limit)
          limit (:body return-resp)]

    (not= 201 (:status return-resp))
    (do (u-ops/-delete user-table user-uid)
        (when (not= 200 (-> rating-service
                            (rating-api/-delete-user-rating (:uid rating))
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the return service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the return service."}}
          :else         {:status 500
                         :body {:message "Error during the return service call."}}))

    :else
    {:status 201
     :body user
     :headers {"Location" (str (remove-trailing-slash service-uri)
                               "/api/users/" user-uid)}}))

(defn get-user
  [{{{:keys [uid]}      :path}   :parameters
    {{user-table :user} :tables} :db}]
  (if-let [user (u-ops/-get user-table uid)]
    {:status 200
     :body user}
    {:status 404
     :body {:message (str "User with uid `" uid "` is not found.")}}))

(defn get-all-users
  [{{{user-table :user} :tables} :db}]
  (let [users (u-ops/-get-all user-table)]
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
    (try (if-let [user (u-ops/-update user-table uid user)]
           {:status 200
            :body user}
           {:status 404
            :body {:message (str "User with uid `" uid "` is not found.")}})
         (catch Exception e
           {:status 400
            :body {:type (-> e type str)
                   :message (ex-message e)}}))))

(defn delete-user
  [{{{:keys [uid]}      :path}   :parameters
    {{user-table :user} :tables} :db
    {library-service    :library
     rating-service     :rating
     return-service     :return} :services}]  
  (b/cond
    :let [user (u-ops/-delete user-table uid)]

    (nil? user)
    {:status 404
     :body {:message (str "User with uid `" uid "` is not found.")}}

    :let [rating-resp (-> rating-service
                          (rating-api/-delete-user-rating-by-user-uid uid)
                          :status)]

    (not= 200 rating-resp)
    (do (u-ops/-restore user-table uid)
        (match rating-resp
          (:or 500 503) {:status 502
                         :body {:message "Error during the rating service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the rating service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the rating service."}}
          :else         {:status 500
                         :body {:message "Error during the rating service call."}}))

    :let [return-resp (-> return-service
                          (return-api/-delete-user-limit-by-user-uid uid)
                          :status)]

    (not= 200 return-resp)
    (do (u-ops/-restore user-table uid)
        (when (not= 200 (-> rating-service
                            (rating-api/-restore-user-rating-by-user-uid uid)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match return-resp
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the return service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the return service."}}
          :else         {:status 500
                         :body {:message "Error during the return service call."}}))
    
    :let [library-resp (-> library-service
                           (library-api/-update-all-orders {:user-uid uid} {:user-uid nil})
                           :status)]
    
    (not= 200 library-resp)
    (do (u-ops/-restore user-table uid)
        (when (not= 200 (-> rating-service
                            (rating-api/-restore-user-rating-by-user-uid uid)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (when (not= 200 (-> return-service
                            (return-api/-restore-user-limit-by-user-uid uid)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match library-resp
          (:or 500 503) {:status 502
                         :body {:message "Error during the library service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the library service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the library service."}}
          :else         {:status 500
                         :body {:message "Error during the library service call."}}))

    :else
    {:status 200
     :body user}))

(defn restore-user
  "User restore doesn't restore relations within orders!"
  [{{{:keys [uid]}      :path}   :parameters
    {{user-table :user} :tables} :db
    {rating-service     :rating
     return-service     :return} :services}]
  (b/cond
    :let [user (u-ops/-restore user-table uid)]

    (nil? user)
    {:status 404
     :body {:message (str "User with uid `" uid "` is not found.")}}

    :let [rating-resp (-> rating-service
                          (rating-api/-restore-user-rating-by-user-uid uid)
                          :status)]
   
    (not= 200 rating-resp)
    (do (u-ops/-delete user-table uid)
        (match rating-resp
          (:or 500 503) {:status 502
                         :body {:message "Error during the rating service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the rating service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the rating service."}}
          :else         {:status 500
                         :body {:message "Error during the rating service call."}}))

    :let [return-resp (-> return-service
                          (return-api/-restore-user-limit-by-user-uid uid)
                          :status)]
    
    (not= 200 return-resp)
    (do (u-ops/-delete user-table uid)
        (when (not= 200 (-> rating-service
                            (rating-api/-delete-user-rating-by-user-uid uid)
                            :status))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match return-resp
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to acces the return service due to invalid credentials."}}
          400           {:status 500
                         :body {:message "Malformed request to the return service."}}
          :else         {:status 500
                         :body {:message "Error during the return service call."}}))

    :else
    {:status 200
     :body user}))

(defn get-tokens
  [{{{:keys [email password]} :body}   :parameters
    {{user-table :user}       :tables} :db}]
  (if-let [user (u-ops/-get-by-email user-table email)]
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
  (try (let [{:keys [uid]} (auth/unsign-jwt refresh-token)
             uid (java.util.UUID/fromString uid)]
         (if-let [user (u-ops/-get user-table uid)]
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
