(ns algopop.leaderboardx.app.views.settings
  (:require
   [algopop.leaderboardx.app.communication :as comm]
   [reagent.session :as session]))

(defn settings-view []
  [:div
   [:h1 "hello"]
   [:input {:type "text"}]
   [:button.btn.btn-default
    {:on-click (fn [e]
                 (session/assoc-in! [:viewpoint :route :foo] "bar")
                 (prn "S" session/state)
                 (comm/chsk-send! [:leaderboardx/hello "hi"]))}
    "doit"]])
