(ns service.gateway.system
  (:require [org.httpkit.server :refer [run-server]])
  (:gen-class))

(def app
  (constantly
   {:status 200 :body "gateway"}))

(defn -main [& args]
  (run-server #'app {:port 3000}))