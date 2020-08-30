(ns algopop.leaderboardx.app.logging
  (:require [clojure.string :as str]
            [reagent.session :as session]
            [taoensso.encore :as encore]))

(defn debug [& args]
  (encore/log (str/join " " args)))

(defn error [e & args]
  (let [message (str (str/join " " args) ": " (pr-str e))]
    (session/update-in! [:errors] conj message)
    (encore/log message)))
