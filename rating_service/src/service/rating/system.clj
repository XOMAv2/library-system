(ns service.rating.system
  (:require [org.httpkit.server :refer [run-server]])
  (:gen-class))

(def app
  (constantly
   {:status 200 :body "rating"}))

(defn -main [& args]
  (run-server #'app {:port 3000}))
