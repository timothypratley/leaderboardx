(ns algopop.leaderboardx.server.main
  (:require
    [org.httpkit.server :refer [run-server]]
    [compojure.route :refer [not-found resources]]
    [compojure.handler :refer [site]]
    [compojure.core :refer [defroutes GET POST DELETE ANY context]]
    [environ.core :refer [env]]
    [ring.util.response :as resp])
  (:gen-class))

(defroutes all-routes
           (GET "/" [] (resp/content-type (resp/resource-response "index.html" {:root "public"}) "text/html"))
           (resources "/")
           (not-found "<p>Page not found.</p>")) ;; all other, return 404

(defn -main [& args]
  (run-server (site #'all-routes) {:port (Integer. (env :port 3000))})
  (println "Server started."))
