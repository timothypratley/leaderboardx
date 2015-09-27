(ns algopop.leaderboardx.app.commands
  (:require [reagent.session :as session]))

(defn save [m]
  (let [now (js/Date.)]
    (session/assoc-in! [:viewpoint :route :commands now] m)
    now))
