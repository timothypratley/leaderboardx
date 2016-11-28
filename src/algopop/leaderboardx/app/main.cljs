(ns algopop.leaderboardx.app.main
  (:require
    [algopop.leaderboardx.app.db]
    [algopop.leaderboardx.app.routes :as routes]
    [algopop.leaderboardx.app.views.header :as header]
    [goog.dom :as dom]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as reagent]
    [reagent.session :as session])
  (:import goog.History))

(defn container []
  [:div.container
   [header/header]
   [:div.well
    [routes/page (session/get-in [:viewpoint :route])]]
   [:div.well
    [:div {:id "disqus_thread"}]]])

(defn navigation [event]
  (session/assoc-in! [:viewpoint :route] (routes/match (.-token event))))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen EventType/NAVIGATE navigation)
    (.setEnabled true)))

(defn mount-root []
  (reagent/render [#'container] (dom/getElement "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))

(defn ^:export main []
  (init!))
