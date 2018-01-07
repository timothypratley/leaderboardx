(ns algopop.leaderboardx.app.main
  (:require
    [algopop.leaderboardx.app.db]
    [algopop.leaderboardx.app.firebase :as firebase]
    [algopop.leaderboardx.app.routes :as routes]
    [algopop.leaderboardx.app.views.header :as header]
    [cljsjs.bootstrap]
    [goog.dom :as dom]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as reagent]
    [reagent.session :as session]
    [recalcitrant.core :refer [error-boundary]])
  (:import
    goog.History))

(defn container []
  [error-boundary
   [:div.container
    {:style {:margin "1%"
             :padding "1%"
             :width "98%"}}
    [header/header]
    [:div.well
     [error-boundary
      [routes/current-page]]]]])

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
