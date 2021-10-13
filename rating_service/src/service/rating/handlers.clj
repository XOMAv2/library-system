(ns service.rating.handlers
  (:require [service.rating.tables.user-rating :as ur-ops]
            [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.return :as return-api]
            [better-cond.core :as b]
            [clojure.core.match :refer [match]]))

(defn rating->limit [r]
  (cond
    (< r 5) 1
    (< r 10) 2
    (< r 15) 3
    (< r 20) 4
    :else 5))

(defn condition->delta [c]
  (case c
    "normal" 2
    "poor" -2
    "terrible" -5
    (throw (Exception. "Unknown book condition."))))

(defn add-user-rating
  [{{user-rating                      :body}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {service-uri                      :rating} :services-uri
    {return-service                   :return} :services}]
  (b/cond
    :let [user-rating (try (ur-ops/-add user-rating-table user-rating)
                           (catch Exception e e))]

    (instance? Exception user-rating)
    (let [e user-rating]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})

    :let [limit (rating->limit (:rating user-rating))
          user-limit {:user-uid (:user-uid user-rating)
                      :total-limit limit
                      :available-limit limit}
          return-resp (return-api/-add-user-limit return-service user-limit)]

    (not= 201 (:status return-resp))
    (do (when (nil? (try (ur-ops/-delete user-rating-table (:user-uid user-rating))
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          422           {:status 422
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 201
     :body user-rating
     :headers {"Location" (str (remove-trailing-slash service-uri)
                               "/api/ratings/"
                               (:uid user-rating))}}))

(defn get-user-rating
  [{{{:keys [uid]}                    :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (if-let [user-rating (ur-ops/-get user-rating-table uid)]
    {:status 200
     :body user-rating}
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}))

(defn get-user-rating-by-user-uid
  [{{{:keys [user-uid]}               :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db}]
  (if-let [user-rating (ur-ops/-get-by-user-uid user-rating-table user-uid)]
    {:status 200
     :body user-rating}
    {:status 404
     :body {:message (str "User rating with user uid `" user-uid "` is not found.")}}))

(defn get-all-user-ratings
  [{{{user-rating-table :user-rating} :tables} :db}]
  (let [user-ratings (ur-ops/-get-all user-rating-table)]
    {:status 200
     :body {:ratings user-ratings}}))

(defn update-user-rating
  [{{{:keys [uid]}                    :path
     user-rating                      :body}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {return-service                   :return} :services}]
  (b/cond
    :let [prev-user-rating (ur-ops/-get user-rating-table uid)]

    (nil? prev-user-rating)
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}

    :let [rating (:rating user-rating)
          user-rating (try (ur-ops/-update user-rating-table uid user-rating)
                           (catch Exception e e))]

    (instance? Exception user-rating)
    (let [e user-rating]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})

    (nil? user-rating)
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}

    (nil? rating)
    {:status 200
     :body user-rating}

    :let [limit (rating->limit rating)
          user-uid (:user-uid user-rating)
          return-resp (return-api/-reset-total-limit-by-user-uid return-service
                                                                 user-uid limit)]

    (not= 200 (:status return-resp))
    (do (when (nil? (try (ur-ops/-update user-rating-table uid prev-user-rating)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          422           {:status 422
                         :body (:body return-resp)}
          404           {:status 404
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 200
     :body user-rating}))

(defn update-rating-by-user-uid
  [{{{:keys [user-uid condition]}     :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {return-service                   :return} :services}]
  (b/cond
    :let [user-rating-prev (ur-ops/-get-by-user-uid user-rating-table user-uid)]

    (nil? user-rating-prev)
    {:status 404
     :body {:message (str "User rating with user uid `" user-uid "` is not found.")}}
   
    :let [delta (condition->delta condition)
          delta (let [delta2 (+ (:rating user-rating-prev) delta)]
                  (if (neg? delta2)
                    (- delta delta2)
                    delta))
          user-rating (try (ur-ops/-update-rating-by-user-uid user-rating-table
                                                              user-uid delta)
                           (catch Exception e e))]

    (instance? Exception user-rating)
    (let [e user-rating]
      {:status 422
       :body {:type (-> e type str)
              :message (ex-message e)}})

    (nil? user-rating)
    {:status 404
     :body {:message (str "User rating with user uid `" user-uid "` is not found.")}}

    :let [limit (rating->limit (:rating user-rating))
          return-resp (return-api/-reset-total-limit-by-user-uid return-service
                                                                 user-uid limit)]
   
    (not= 200 (:status return-resp))
    (do (when (nil? (try (ur-ops/-update-rating-by-user-uid user-rating-table
                                                            user-uid (- delta))
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          422           {:status 422
                         :body (:body return-resp)}
          404           {:status 404
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 200
     :body user-rating}))

(defn delete-user-rating
  [{{{:keys [uid]}                    :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {return-service                   :return} :services}]
  (b/cond
    :let [user-rating (ur-ops/-delete user-rating-table uid)
          user-uid (:user-uid user-rating)]

    (nil? user-rating)
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}

    :let [return-resp (return-api/-delete-user-limit-by-user-uid return-service user-uid)]

    (not= 200 (:status return-resp))
    (do (when (nil? (try (ur-ops/-restore user-rating-table uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          404           {:status 404
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 200
     :body user-rating}))

(defn delete-user-rating-by-user-uid
  [{{{:keys [user-uid]}               :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {return-service                   :return} :services}]
  (b/cond
    :let [user-rating (ur-ops/-delete-by-user-uid user-rating-table user-uid)]

    (nil? user-rating)
    {:status 404
     :body {:message (str "User rating with user uid `" user-uid "` is not found.")}}

    :let [return-resp (return-api/-delete-user-limit-by-user-uid return-service user-uid)]

    (not= 200 (:status return-resp))
    (do (when (nil? (try (ur-ops/-restore-by-user-uid user-rating-table user-uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          404           {:status 404
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 200
     :body user-rating}))

(defn restore-user-rating
  [{{{:keys [uid]}                    :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {return-service                   :return} :services}]
  (b/cond
    :let [user-rating (ur-ops/-restore user-rating-table uid)
          user-uid (:user-uid user-rating)]

    (nil? user-rating)
    {:status 404
     :body {:message (str "User rating with uid `" uid "` is not found.")}}

    :let [return-resp (return-api/-restore-user-limit-by-user-uid return-service user-uid)]

    (not= 200 (:status return-resp))
    (do (when (nil? (try (ur-ops/-delete user-rating-table uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          404           {:status 404
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 200
     :body user-rating}))

(defn restore-user-rating-by-user-uid
  [{{{:keys [user-uid]}               :path}   :parameters
    {{user-rating-table :user-rating} :tables} :db
    {return-service                   :return} :services}]
  (b/cond
    :let [user-rating (ur-ops/-restore-by-user-uid user-rating-table user-uid)]

    (nil? user-rating)
    {:status 404
     :body {:message (str "User rating with user uid `" user-uid "` is not found.")}}

    :let [return-resp (return-api/-restore-user-limit-by-user-uid return-service user-uid)]

    (not= 200 (:status return-resp))
    (do (when (nil? (try (ur-ops/-delete-by-user-uid user-rating-table user-uid)
                         (catch Exception _ nil)))
          #_"TODO: do something when api call returns bad response and "
          #_"we are already processing bad response branch.")
        (match (:status return-resp)
          (:or 500 503) {:status 502
                         :body {:message "Error during the return service call."
                                :response return-resp}}
          (:or 401 403) {:status 500
                         :body {:message "Unable to access the return service due to invalid credentials."
                                :response return-resp}}
          404           {:status 404
                         :body (:body return-resp)}
          :else         {:status 500
                         :body {:message "Error during the return service call."
                                :response return-resp}}))

    :else
    {:status 200
     :body user-rating}))
