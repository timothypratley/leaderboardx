(ns algopop.leaderboardx.app.io.common
  (:require [clojure.string :as str]))

(defn quote-escape [s]
  (str \" (str/replace s #"\"" "\\\"") \"))

(def option-keys
  [:show-pageranks? :straight-edges? :collapse-reciprocal?])
