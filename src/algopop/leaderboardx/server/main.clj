(ns algopop.leaderboardx.server.main
  (:require
    [org.httpkit.server :refer [run-server]]
    [compojure.route :refer [not-found resources]]
    [compojure.handler :refer [site]]
    [compojure.core :refer [defroutes GET POST DELETE ANY context]]
    [environ.core :refer [env]]
    [ring.util.response :refer [content-type resource-response redirect]])
  (:gen-class))

(defroutes all-routes
  (ANY "*" []
    (redirect "https://timothypratley.github.io/leaderboardx/")))

(defn -main [& args]
  (run-server (site #'all-routes) {:port (Integer. (env :port 3000))})
  (println "Server started."))
