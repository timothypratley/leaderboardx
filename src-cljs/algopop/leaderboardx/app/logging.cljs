(ns algopop.leaderboardx.app.logging
  (:require [clojure.string :as string]
            [taoensso.encore :as encore]))

(defn debug [& args]
  (encore/log (string/join " " args)))
