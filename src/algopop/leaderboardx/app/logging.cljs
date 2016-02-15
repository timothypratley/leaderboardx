(ns algopop.leaderboardx.app.logging
  (:require [clojure.string :as string]
            [reagent.session :as session]
            [taoensso.encore :as encore]))

(defn debug [& args]
  (encore/log (string/join " " args)))

(defn error [e & args]
  (let [message (str (string/join " " args) ": " (pr-str e))]
    (session/update-in! [:errors] conj message)
    (encore/log message)))
