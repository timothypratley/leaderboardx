(ns algopop.leaderboardx.app.io.csv
  (:require [clojure.string :as string]))

(defn write-graph [g]
  (string/join
   \newline
   (cons
    "Person, Commends, Commended by"
    (for [[from tos] (:edges g)]
      (str from ", "
           (string/join "; " (keys tos))
           (string/join "; " ()))))))

(defn read-graph [csv]
  (println "CSV" csv)
  {})

;; TODO: use semicolons in column
;; alow UI to use either
