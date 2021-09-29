(ns service.frontend.events
  (:require [re-frame.core :as rf]
            [service.frontend.db :as db]))

(rf/reg-event-db ::initialize-db
  (fn [_ _]
      db/default-db))
