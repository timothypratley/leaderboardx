(ns algopop.leaderboardx.app.views.coach-player
  (:require [reagent.core :as reagent]
            [algopop.leaderboardx.app.db :as db]))

(defn transpose [xs]
  (apply mapv vector xs))

(def player
  (reagent/atom
    [["William" "April" "May"]
     ["Productivity" 7 8]
     ["Leadership" 5 5]
     ["Effort" 7 8]
     ["Attitude" 5 7]
     ["Happiness" 5 4]]))

(defn performance-table [[headers & data-rows]]
  [:table.table.table-responsive
   [:thead
    (into [:tr]
          (cons [:th]
                (for [date (rest headers)]
                  [:th date])))]
   (into [:tbody]
         (for [[attr & vs] data-rows]
           (into [:tr]
                 (cons [:th attr]
                       (for [v vs]
                         [:td v])))))])

(defn acheivements [x]
  [:div
   [:h4 "Acheivements"]
   [:ul
    [:li "Won the spelling bee"]
    [:li "April: learnt the yoyo"]]])

(defn needs-improvement [x]
  [:div
   [:h4 "Needs improvement"]
   [:ul
    [:li "Sprint times"]]])

(defn goals []
  [:div
   [:h4 "Goals"]
   [:ul
    [:li "Train sprinting every second day"]]])

(defn coach-comments []
  [:div
   [:h4 "Coach comments"]])

(defn player-comments []
  [:div
   [:h4 "Player comments"]])

(defn coach-player-view []
  (let [p (db/player)]
    (fn a-coach-player-view []
      [:div
       [:h1 (:name (ffirst @p))]
       [performance-table @player]
       [acheivements @player]
       [needs-improvement @player]
       [goals]
       [coach-comments @player]
       [player-comments @player]])))
