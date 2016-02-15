(ns algopop.leaderboardx.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (println "Starting datomic-db" uri)
    (let [db (d/create-database uri)
          conn (d/connect uri)]
      (assoc component :conn conn)))
  (stop [component]
    (assoc component :conn nil)))

(defn new-datomic-db [uri]
  (map->Datomic {:uri uri}))
