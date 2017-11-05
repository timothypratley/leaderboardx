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
  (:import goog.History))

(defn container []
  [error-boundary
   [:div.container
    [header/header]
    [:div.well
     [error-boundary
      [routes/current-page]]]
    [:div.well
     [:div {:id "disqus_thread"}]]]])

(defonce history
  (History.))

;; May be called multiple times due to code reloading
(defn hook-browser-navigation! []
  (doto history
    (events/removeAll)
    (events/listen EventType/NAVIGATE routes/navigate)
    (.setEnabled true)))

(defn mount-root []
  (reagent/render [container] (dom/getElement "app")))

(defn init! []
  (firebase/init)
  (hook-browser-navigation!)
  (mount-root))

(defn ^:export main []
  (init!))
