(ns service.frontend.db
  (:require [service.frontend.views :as views]))

(def default-db
  {:ui-state {:view [views/login-view]
              :view-scope nil
              :modal nil
              :modal-scope nil}
   :user-uid nil
   :tokens {:access-token nil
            :refresh-token nil}
   :entities {:users {}
              :libraries {}
              :books {}}})
