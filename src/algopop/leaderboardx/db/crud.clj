(ns algopop.leaderboardx.db.crud
  (:require [algopop.leaderboardx.db :as db]))

(defn cu [m]
  (db/transact [(cond-> m
                  (not (:db/id m)) (assoc :db/id #db/id[:db.part/user]))]))

(defn r [id]
  (db/pull-id id))

(defn d [id]
  (db/transact [[:db.fn/retractEntity id]]))
