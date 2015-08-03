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

(defn prod-system []
  (component/system-map
   :sente (new-channel-sockets event-msg-handler sente-web-server-adapter)
   :web (new-web-server 3000 #_(env :http-port) handler)
   :datomic-db (new-datomic-db "datomic:free://localhost:4334/mbrainz-1968-1973" #_(env :db-url))
   :repl-server (new-repl-server 3001 #_(env :repl-port))))
