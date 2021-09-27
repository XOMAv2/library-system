(ns utilities.api.stats
  (:require [utilities.core :refer [remove-trailing-slash]]
            [utilities.api.core :refer [cb-sync-request make-cb make-request]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb])
  (:import [net.jodah.failsafe CircuitBreaker]))

(defprotocol StatsAPI
  (-add-stat-record [this stat-record] "")
  (-get-stat-record [this uid] "")
  (-get-all-stat-records [this] [this service] "")
  (-update-stat-record [this uid stat-record] "")
  (-delete-stat-record [this uid] "")
  (-delete-all-stat-records [this] "")
  (-get-token [this] "")
  (-refresh-token [this] "")
  (-verify-token [this] ""))

(def ^:const default-exchange-name "")

(defrecord StatsService [uri
                         ^CircuitBreaker cb
                         ^clojure.lang.Atom token
                         client-id client-secret
                         amqp-url qname]
  StatsAPI
  (-add-stat-record [this stat-record]
    (let [conn (rmq/connect {:uri amqp-url})
          ch (lch/open conn)]
      (lb/publish ch default-exchange-name qname (str stat-record)
                  {:content-type "application/edn; charset=utf-8"})
      (rmq/close ch)
      (rmq/close conn)))
  (-get-stat-record [this uid]                (make-request :get (str "/api/stats/" uid)))
  (-get-all-stat-records [this]               (make-request :get "/api/stats"))
  (-get-all-stat-records [this service]       (make-request :get "/api/stats" nil {:service service}))
  (-update-stat-record [this uid stat-record] (make-request :patch (str "/api/stats/" uid) stat-record))
  (-delete-stat-record [this uid]             (make-request :delete (str "/api/stats/" uid)))
  (-delete-all-stat-records [this]            (make-request :delete "/api/stats"))
  (-get-token [this]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/login")
                         :headers {"Content-Type" "application/edn; charset=utf-8"
                                   "Accept" "application/edn; charset=utf-8"}
                         :body (str {:client-id client-id
                                     :client-secret client-secret})}))
  (-refresh-token [this]
    (cb-sync-request cb {:method :put
                         :url (str (remove-trailing-slash uri) "​/api​/auth​/refresh")
                         :headers {"Authorization" (str "Bearer " @token)
                                   "Accept" "application/edn; charset=utf-8"}}))
  (-verify-token [this]
    (cb-sync-request cb {:method :post
                         :url (str (remove-trailing-slash uri) "/api/auth/verify")
                         :headers {"Authorization" (str "Bearer " @token)
                                   "Accept" "application/edn; charset=utf-8"}})))

(defn make-stats-service [uri cb-options client-id client-secret rabbit-opts]
  (->StatsService uri
                  (make-cb cb-options)
                  (atom nil)
                  client-id client-secret
                  (:amqp-url rabbit-opts) (:qname rabbit-opts)))
