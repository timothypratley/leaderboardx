(ns algopop.leaderboardx.datomic
  (:require [algopop.leaderboardx.db.schema :as s]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (let [db (d/create-database uri)
          conn (d/connect uri)]
      ;;(s/migrate conn)
      (assoc component :conn conn)))
  (stop [component]
    (assoc component :conn nil)))

(defn new-datomic-db [uri]
  (map->Datomic {:uri uri}))
