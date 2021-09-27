(ns service.gateway.middlewares
  (:require [malli.core :as m]
            [clojure.spec.alpha :as s]
            [utilities.api.session :refer [SessionAPI] :as session-api]
            [buddy.auth.backends.token]))

(defn- wrap-authentication
  "In case of successful request authentication `authenticate-fn` must return any value
   other than nil."
  [handler authenticate-fn]
  (fn [request]
    (let [request (if-let [authdata (authenticate-fn request)]
                    (assoc request :identity authdata)
                    request)]
      (handler request))))

(s/def ::services (m/validator [:map [:session [:fn (fn [x] (satisfies? SessionAPI x))]]]))
(s/def ::auth (m/validator [:map [:token-name [:maybe string?]]]))

(def authentication-middleware
  "Obtaining data from authorization header into :identity request slot."
  {:name ::authorization-middleware
   :spec (s/keys :req-un [::services]
                 :opt-un [::auth])
   :compile (fn [{{session-service :session} :services
                  {token-name :token-name :or {token-name "Bearer"}} :auth} _]
              (let [authenticate-fn
                    (fn [request]
                      (let [token (#'buddy.auth.backends.token/parse-header request token-name)
                            session-resp (session-api/-verify-token session-service token)]
                        (when (= 200 (:status session-resp))
                          (:body session-resp))))]
                {:wrap #(wrap-authentication % authenticate-fn)}))})
