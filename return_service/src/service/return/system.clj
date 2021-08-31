(ns service.return.system
  (:require [org.httpkit.server :refer [run-server]])
  (:gen-class))

(def app
  (constantly
   {:status 200 :body "return"}))

(defn -main [& args]
  (run-server #'app {:port 3000}))
