(ns algopop.leaderboardx.app.io.common
  (:require [clojure.string :as string]))

(defn quote-escape [s]
  (str \" (string/replace s #"\"" "\\\"") \"))
