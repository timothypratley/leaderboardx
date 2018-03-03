(ns algopop.leaderboardx.graph.algo
  (:require [algopop.leaderboardx.graph.shortest-path :as sp]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [clojure.string :as string]
            [algopop.leaderboardx.graph.graph :as graph]))

(defn shortest-path [g selected-id]
  (reagent/with-let
    [from (reagent/atom "")
     to (reagent/atom "")
     watch (ratom/reaction
             (when (string? @selected-id)
               (if (string/blank? @from)
                 (reset! from @selected-id)
                 (reset! to @selected-id))))]
    @watch
    [:div
     "Shortest Path"
     [:input
      {:value @from
       :on-change
       (fn [e]
         (reset! from (.. e -target -value)))}]
     [:input
      {:value @to
       :on-change
       (fn [e]
         (reset! to (.. e -target -value)))}]
     [:button.btn.btn-default
      {:on-click
       (fn [e]
         (sp/shortest-path g @from @to (atom true)))}
      "Search"]]))

(defn page-rank [g selected-id]
  [:div
   [:button.btn.btn-default
    {:on-click
     (fn [e]
       (swap! g graph/with-ranks))}
    "Page Rank"]])

(defn algos [g selected-id]
  [:div
   [shortest-path g selected-id]
   [page-rank g selected-id]])