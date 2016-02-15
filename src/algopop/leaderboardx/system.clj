(ns algopop.leaderboardx.system
  (:require
   [algopop.leaderboardx.routes :refer [handler]]
   [algopop.leaderboardx.communication :refer [event-msg-handler]]
   [algopop.leaderboardx.http-kit :refer [new-web-server]]
   [algopop.leaderboardx.sente :refer [new-channel-sockets]]
   [algopop.leaderboardx.repl-server :refer [new-repl-server]]
   [algopop.leaderboardx.datomic :refer [new-datomic-db]]
   [com.stuartsierra.component :as component]
   [environ.core :refer [env]]
   [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]))

(def datomic-uri "datomic:dev://localhost:4334/leaderboardx")

(defn prod-system []
  (component/system-map
   :sente (new-channel-sockets
           event-msg-handler
           sente-web-server-adapter)
   :web (new-web-server
         (env :http-port 3000) handler)
   :repl-server (new-repl-server
                 (env :repl-port 3001))
   :datomic-db (new-datomic-db
                (env :db-url datomic-uri))))
