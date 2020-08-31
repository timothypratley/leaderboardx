(ns algopop.leaderboardx.app.io.common
  (:require [clojure.string :as string]))

(defn quote-escape [s]
  (str \" (string/replace s #"\"" "\\\"") \"))

(def option-keys
  [:straight-edges? :collapse-reciprocal? :scale-by])
