(ns utilities.auth
  (:require [buddy.auth.backends :as backends]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]
            [utilities.time :as time]
            [utilities.schemas :as schemas]
            [malli.core :as m]
            [clojure.spec.alpha :as s]))
  
(def jwt-secret "JTW_SECRET")

(def backend (backends/jws {:secret jwt-secret
                            :token-name "Bearer"}))

(defn sign-jwt-access [credentials]
  (-> credentials
      (assoc :exp (time/add (time/now) (time/hours 1)))
      (jwt/sign jwt-secret)))

(defn sign-jwt-refresh [credentials]
  (-> credentials
      (assoc :exp (time/add (time/now) (time/months 1)))
      (jwt/sign jwt-secret)))

(defn unsign-jwt [token]
  (jwt/unsign token jwt-secret))

(defn- wrap-authorization
  "`roles` should be set of allowed roles or nil if any role is allowed."
  ([handler]
   (wrap-authorization handler nil))
  ([handler roles]
   (fn [request]
     (let [role (-> request :identity :role)]
       (cond
         (and (authenticated? request)
              (or (nil? roles)
                  (contains? roles role)))
         (handler request)

         (authenticated? request)
         {:status 403
          :body {:message (str "Role `" role "` is not appropriate for executing the request.")}}

         :else
         {:status 401
          :body {:message "Unauthorized"}})))))

(def swagger-msg-obj
  {:schema {:type "object"
            :properties {:message {:type "string"}}
            :required ["message"]}
   :description ""})

(s/def ::roles (s/nilable (s/and set?
                                 not-empty
                                 (s/every (m/validator schemas/role)))))

(def authorization-middleware
  {:name ::authorization-middleware
   :spec (s/keys :req-un [::roles])
   :compile (fn [{{security-defs :securityDefinitions} :swagger
                  roles                                :roles} _]
              (merge
               (when security-defs
                 {:data {:swagger
                         {:security (->> security-defs
                                         (keys)
                                         (map #(vector % []))
                                         (into {})
                                         (vector))
                          :responses (merge
                                      (when roles {403 swagger-msg-obj})
                                      {401 swagger-msg-obj})}}})
               {:wrap #(wrap-authorization % roles)}))})
