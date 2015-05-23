(ns algopop.leaderboardx.app.io.csv
  (:require [clojure.string :as string]))

(defn write-graph [g]
  (string/join
   \newline
   (cons
    "Person,Commends"
    (for [[from tos] (:edges g)]
      (string/join "," (cons from (keys tos)))))))

(defn read-graph [csv]
  (println "CSV" csv)
  {})
