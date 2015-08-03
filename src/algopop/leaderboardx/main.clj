(ns algopop.leaderboardx.main
  (:require [algopop.leaderboardx.system :refer [prod-system]]
            [reloaded.repl :as reloaded])
  (:gen-class))

(defn -main [& args]
  (reloaded/set-init! prod-system)
  (reloaded/go)
  (println "Server started."))
