(ns algopop.leaderboardx.repl-server
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [cider.nrepl :refer [cider-nrepl-handler]]))

(defrecord ReplServer [port server]
  component/Lifecycle
  (start [component]
    (println "Starting repl on port" port)
    (assoc component :server (start-server :port port
                                           :handler cider-nrepl-handler)))
  (stop [component]
    (when server
      (stop-server server)
      component)))

(defn new-repl-server [port]
  (map->ReplServer {:port port}))
