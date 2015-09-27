(ns algopop.leaderboardx.db
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [reloaded.repl :refer [system]]))

(defn conn []
  (or (:conn (:datomic-db system))
      (throw (ex-info "Not connected" (:datomic-db system)))))

(defn db []
  (d/db (conn)))

(defn pull [k name]
  (d/pull
   (db)
   '[*]
   (ffirst
    (d/q
     (format
      "[:find ?e
        :in $ ?name
        :where [$ ?e %s ?name]]"
      k)
     (db)
     name))))

(defn pull-q [q]
  (d/pull
   (db)
   '[*]
   (ffirst (d/q q (db)))))

(defn pull-id [id]
  (d/pull (db) '[*] id))

(defn q [q]
  (d/q q (db)))

(defn transact [xs]
  (d/transact (conn) xs))
