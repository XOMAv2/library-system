(ns server.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [resource-response]])
  (:gen-class))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (resources "/" {:root "public"})
  (not-found "Page not found"))

(defn -main [& args]
  (run-jetty #'routes {:port 5000
                       :join? false}))
