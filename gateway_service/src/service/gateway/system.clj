(ns service.gateway.system
  (:require [org.httpkit.server :refer [run-server]]
            [service.gateway.router :refer [app]]
            [utilities.config :refer [load-config]]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [malli.core :as m]
            [utilities.core :refer [non-empty-string?]]
            [utilities.schemas :as schemas]
            [utilities.api.book :refer [BookAPI make-book-service]]
            [utilities.api.library :refer [LibraryAPI make-library-service]]
            [utilities.api.rating :refer [RatingAPI make-rating-service]]
            [utilities.api.return :refer [ReturnAPI make-return-service]]
            [utilities.api.session :refer [SessionAPI make-session-service]]
            [utilities.api.stats :refer [StatsAPI make-stats-service]])
  (:gen-class))

#_"ig/init-key"

(defmethod ig/init-key :service.gateway.system/services
  [_ {:keys [rabbit-opts services-uri cb-options client-id client-secret]}]
  {:book (make-book-service (:book services-uri) cb-options client-id client-secret)
   :library (make-library-service (:library services-uri) cb-options client-id client-secret)
   :rating (make-rating-service (:rating services-uri) cb-options client-id client-secret)
   :return (make-return-service (:return services-uri) cb-options client-id client-secret)
   :session (make-session-service (:session services-uri) cb-options)
   :stats (make-stats-service (:stats services-uri) cb-options client-id client-secret rabbit-opts)})

(defmethod ig/init-key :service.gateway.system/app [_ {:keys [services client-id]}]
  (app {:services services
        :client-id client-id}))

(defmethod ig/init-key :service.gateway.system/server [_ {:keys [app server-options]}]
  (run-server app server-options))

#_"ig/halt-key!"

(defmethod ig/halt-key! :service.gateway.system/server [_ server]
  (server :timeout 100))

#_"ig/pre-init-spec"

(s/def ::services (m/validator [:map
                                [:book [:fn (fn [x] (satisfies? BookAPI x))]]
                                [:library [:fn (fn [x] (satisfies? LibraryAPI x))]]
                                [:rating [:fn (fn [x] (satisfies? RatingAPI x))]]
                                [:return [:fn (fn [x] (satisfies? ReturnAPI x))]]
                                [:session [:fn (fn [x] (satisfies? SessionAPI x))]]
                                [:stats [:fn (fn [x] (satisfies? StatsAPI x))]]]))
(s/def ::services-uri (m/validator schemas/services-uri))
(s/def ::cb-options (m/validator [:map
                                  [:failure-threshold-ratio [:tuple pos-int? pos-int?]]
                                  [:delay-ms nat-int?]]))
(s/def ::client-id non-empty-string?)
(s/def ::client-secret non-empty-string?)
(s/def ::app fn?)
(s/def ::server-options (m/validator schemas/server-options))
(s/def ::qname non-empty-string?)
(s/def ::amqp-url non-empty-string?)
(s/def ::rabbit-opts (s/keys :req-un [::qname ::amqp-url]))

(defmethod ig/pre-init-spec :service.gateway.system/services [_]
  (s/keys :req-un [::rabbit-opts ::services-uri ::cb-options ::client-id ::client-secret]))

(defmethod ig/pre-init-spec :service.gateway.system/app [_]
  (s/keys :req-un [::services ::client-id]))

(defmethod ig/pre-init-spec :service.gateway.system/server [_]
  (s/keys :req-un [::app ::server-options]))

#_"system init & halt!"

(defonce system (atom nil))

(defn start-system [config]
  (when @system (ig/halt! @system))
  (->> config
       (ig/init)
       (reset! system)))

(defn -main [profile & args]
  (->> {:profile (keyword profile)}
       (load-config "config.edn")
       (start-system)))

#_(-main "local")
#_(ig/halt! @system)
