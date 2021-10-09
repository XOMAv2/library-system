(ns service.frontend.router
  (:require [reitit.frontend]
            [reitit.coercion.malli]
            [reitit.coercion]
            [reitit.frontend.easy]
            [service.frontend.config :as config]
            [reitit.spec]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.transform :as mt]
            [reitit.dev.pretty :as pretty]
            [re-frame.core :as rf]
            [service.frontend.events :as-alias events]))

(def routes
  [["/login" {:name ::login
              :controllers [{:start (fn [_]
                                      (when config/debug?
                                        (.log js/console "Entering" ::login))
                                      (rf/dispatch [::events/init-login]))}]}]

   ["/register" {:name ::register
                 :controllers [{:start (fn [_]
                                         (when config/debug?
                                           (.log js/console "Entering" ::register))
                                         (rf/dispatch [::events/init-register]))}]}]

   ["/libraries"
    ["" {:name ::libraries
         :controllers [{:start (fn [_]
                                 (when config/debug?
                                   (.log js/console "Entering" ::libraries))
                                 (rf/dispatch [::events/init-libraries]))}]}]
    ["/" {:name ::library-add
          :controllers [{:start (fn [_]
                                  (when config/debug?
                                    (.log js/console "Entering" ::library-add))
                                  (rf/dispatch [::events/init-library-add]))}]}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}}
     ["" {:name ::library
          :controllers [{:parameters {:path [:uid]}
                         :start (fn [{{uid :uid} :path}]
                                  (when config/debug?
                                    (.log js/console "Entering" ::library))
                                  (rf/dispatch [::events/init-library uid]))}]}]
     ["/edit" {:name ::library-edit
               :controllers [{:parameters {:path [:uid]}
                              :start (fn [{{uid :uid} :path}]
                                       (when config/debug?
                                         (.log js/console "Entering" ::library-edit))
                                       (rf/dispatch [::events/init-library-edit uid]))}]}]]]
   
   ["/books"
    ["" {:name ::books
         :controllers [{:start (fn [_]
                                 (when config/debug?
                                   (.log js/console "Entering" ::books))
                                 (rf/dispatch [::events/init-books]))}]}]
    ["/" {:name ::book-add
          :controllers [{:start (fn [_]
                                  (when config/debug?
                                    (.log js/console "Entering" ::book-add))
                                  (rf/dispatch [::events/init-book-add]))}]}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}}
     ["" {:name ::book
          :controllers [{:parameters {:path [:uid]}
                         :start (fn [{{uid :uid} :path}]
                                  (when config/debug?
                                    (.log js/console "Entering" ::book))
                                  (rf/dispatch [::events/init-book uid]))}]}]
     ["/edit" {:name ::book-edit
               :controllers [{:parameters {:path [:uid]}
                              :start (fn [{{uid :uid} :path}]
                                       (when config/debug?
                                         (.log js/console "Entering" ::book-edit))
                                       (rf/dispatch [::events/init-book-edit uid]))}]}]]]

   ["/users"
    ["" {:name ::users
         :controllers [{:start (fn [_]
                                 (when config/debug?
                                   (.log js/console "Entering" ::users))
                                 (rf/dispatch [::events/init-users]))}]}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}}
     ["" {:name ::user
          :controllers [{:parameters {:path [:uid]}
                         :start (fn [{{uid :uid} :path}]
                                  (when config/debug?
                                    (.log js/console "Entering" ::user))
                                  (rf/dispatch [::events/init-user uid]))}]}]
     ["/edit" {:name ::user-edit
               :controllers [{:parameters {:path [:uid]}
                              :start (fn [{{uid :uid} :path}]
                                       (when config/debug?
                                         (.log js/console "Entering" ::user-edit))
                                       (rf/dispatch [::events/init-user-edit uid]))}]}]]]
   
   ["/orders"
    ["" {:name ::orders
         :controllers [{:start (fn [_]
                                 (when config/debug?
                                   (.log js/console "Entering" ::orders))
                                 (rf/dispatch [::events/init-orders]))}]}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}}
     ["" {:name ::order
          :controllers [{:parameters {:path [:uid]}
                         :start (fn [{{uid :uid} :path}]
                                  (when config/debug?
                                    (.log js/console "Entering" ::order))
                                  (rf/dispatch [::events/init-order uid]))}]}]
     ["/edit" {:name ::order-edit
               :controllers [{:parameters {:path [:uid]}
                              :start (fn [{{uid :uid} :path}]
                                       (when config/debug?
                                         (.log js/console "Entering" ::order-edit))
                                       (rf/dispatch [::events/init-order-edit uid]))}]}]]]
   
   ["/stats"
    ["" {:name ::stats
         :controllers [{:start (fn [_]
                                 (when config/debug?
                                   (.log js/console "Entering" ::stats))
                                 (rf/dispatch [::events/init-stats]))}]}]
    ["/:uid" {:parameters {:path [:map [:uid uuid?]]}}
     ["" {:name ::stat
          :controllers [{:parameters {:path [:uid]}
                         :start (fn [{{uid :uid} :path}]
                                  (when config/debug?
                                    (.log js/console "Entering" ::stat))
                                  (rf/dispatch [::events/init-stat uid]))}]}]
     ["/edit" {:name ::stat-edit
               :controllers [{:parameters {:path [:uid]}
                              :start (fn [{{uid :uid} :path}]
                                       (when config/debug?
                                         (.log js/console "Entering" ::stat-edit))
                                       (rf/dispatch [::events/init-stat-edit uid]))}]}]]]])

(def router
  (reitit.frontend/router
   routes
   {#_#_:data {:coercion reitit.coercion.malli/coercion}
    :compile reitit.coercion/compile-request-coercers #_"Schemas closing, extra keys stripping, ..."
    #_"... transformers adding for json-body, path and query params."
    :validate reitit.spec/validate #_"Routes structure validation."
    :exception pretty/exception #_"Routes structure error explanation."}))

#_"https://github.com/metosin/reitit/issues/334"
(defn coerce-xxx [match schema-key params-key]
  (if-let [schema (get-in match [:data :parameters schema-key])]
    (let [schema (mu/closed-schema schema)
          params (params-key match)
          params (m/decode schema params mt/string-transformer)]
      (when (m/validate schema params)
        (assoc-in match [:parameters schema-key] params)))
    match))

(defn coerce-match [match]
  (when-let [match (coerce-xxx match :path :path-params)]
    (when-let [match (coerce-xxx match :query :query-params)]
      match)))

(defn start-router! []
  (reitit.frontend.easy/start!
   router
   (fn [match]
     (rf/dispatch [::events/on-route-match (coerce-match match)]))
   {:use-fragment true}))
