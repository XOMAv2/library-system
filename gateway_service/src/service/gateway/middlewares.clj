(ns service.gateway.middlewares
  (:require [malli.core :as m]
            [clojure.spec.alpha :as s]
            [utilities.api.session :refer [SessionAPI] :as session-api]
            [clojure.core.match :refer [match]]
            [buddy.auth.backends.token]))

(defn- wrap-authentication
  [handler session-service token-name]
  (fn [request]
    (let [session-resp (->> token-name
                            (#'buddy.auth.backends.token/parse-header request)
                            (session-api/-verify-token session-service))]
      (match (:status session-resp)
        (:or 500 503)     {:status 502
                           :body {:message "Error during the session service call."
                                  :response session-resp}}

        (:or 400 401 403) (handler request)
        200               (->> (:body session-resp)
                               (assoc request :identity)
                               (handler))
        :else             {:status 500
                           :body {:message "Error during the session service call."
                                  :response session-resp}}))))

(s/def ::services (m/validator [:map [:session [:fn (fn [x] (satisfies? SessionAPI x))]]]))
(s/def ::auth (m/validator [:map [:token-name [:maybe string?]]]))

(def authentication-middleware
  "Obtaining data from authorization header into :identity request slot."
  {:name ::authorization-middleware
   :spec (s/keys :req-un [::services]
                 :opt-un [::auth])
   :compile (fn [{{session-service :session} :services
                  {token-name :token-name :or {token-name "Bearer"}} :auth} _]
              {:wrap #(wrap-authentication % session-service token-name)})})
