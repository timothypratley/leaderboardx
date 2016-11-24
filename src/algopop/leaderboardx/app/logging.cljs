(ns algopop.leaderboardx.app.logging
  (:require [clojure.string :as string]
            [taoensso.encore :as encore]))

(defn debug [& args]
  (encore/log (string/join " " args)))

(defn error [e & args]
  (let [message (str (string/join " " args) ": " (pr-str e))]
    (encore/log message)))
