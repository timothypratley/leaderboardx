(ns algopop.leaderboardx.app.main
  (:require ;;[algopop.leaderboardx.app.communication :as communication]
            [algopop.leaderboardx.app.pages :as pages]
            [algopop.leaderboardx.app.views.header :as header]
            [cljsjs.react :as react]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true])
  (:import goog.History))

;; TODO
(when (not :dev)
  (js/ga "create" "UA-40336415-3" "auto")
  (js/ga "send" "pageview"))

;; -------------------------
;; Routes

(defn current-page []
  [:div.container
   [header/header]
   [:div.well
    [(pages/page (session/get-in [:viewpoint :current-page] :graph-editor))]]
   [:div.well
    [:div {:id "disqus_thread"}]]])

;; -------------------------
;; History
;; must be called after routes have been defined

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app

(defn mount-root []
  ;(println "Reloaded")
  (reagent/render [#'current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
