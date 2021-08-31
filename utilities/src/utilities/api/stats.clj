(ns utilities.api.stats
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]))

(defprotocol StatsAPI
  (-add-stat-record [this stat-record] "")
  #_(-get-stat-record [this uid] "")
  #_(-get-all-stat-records [this] "")
  #_(-update-stat-record [this uid stat-record] "")
  #_(-delete-stat-record [this uid] ""))

(def ^:const default-exchange-name "")

(defrecord StatsService [amqp-url qname]
  StatsAPI
  (-add-stat-record [this stat-record]
    (let [conn (rmq/connect {:uri amqp-url})
          ch (lch/open conn)]
      (lb/publish ch default-exchange-name qname (str stat-record)
                  {:content-type "application/edn; charset=utf-8"})
      (rmq/close ch)
      (rmq/close conn))))
