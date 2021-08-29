(ns utilities.auth
  (:require [buddy.auth.backends :as backends]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]
            [utilities.time :as time]))
  
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

(def authorization-middleware
  {:name ::authorization-middleware
   :compile (fn [{{security-defs :securityDefinitions} :swagger} _]
              (merge
               (when security-defs
                 {:data {:swagger
                         {:security (->> security-defs
                                         (keys)
                                         (map #(vector % []))
                                         (into {})
                                         (vector))
                          :responses {401 {:schema {:type "object"
                                                    :properties {:message {:type "string"}}
                                                    :required ["message"]}
                                           :description ""}
                                      403 {:schema {:type "object"
                                                    :properties {:message {:type "string"}}
                                                    :required ["message"]}
                                           :description ""}}}}})
               {:wrap wrap-authorization}))})
