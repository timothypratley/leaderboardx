(ns algopop.leaderboardx.app.main
  (:require [algopop.leaderboardx.app.model.db]
            [algopop.leaderboardx.app.firebase :as firebase]
            [algopop.leaderboardx.app.routes :as routes]
            [algopop.leaderboardx.app.views.header :as header]
            [cljsjs.bootstrap]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent])
  (:import [goog History]))

(set! *warn-on-infer* true)

(defn container []
  [:div.container
   [header/header]
   [:div.well
    [routes/current-page]]])

(defonce history
  (doto (History.)
    (events/removeAll)
    (events/listen EventType/NAVIGATE (fn on-navigate [e]
                                        (#'routes/navigate e)))
    (.setEnabled true)))

(defn mount-root []
  (reagent/render [container] (dom/getElement "app")))

(defn init! []
  (firebase/init)
  (mount-root))

(defn ^:export main []
  (init!))
