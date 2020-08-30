(ns algopop.leaderboardx.graph.algorithm-selection-view
  (:require [algopop.leaderboardx.graph.shortest-path :as sp]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [clojure.string :as str]))

;; TODO: show a sidebar of the state of the search
;; TODO: show the candidates as a set and as a priority queue
(defn shortest-path [g selected-id]
  (reagent/with-let
    [from (reagent/atom "")
     to (reagent/atom "")
     step-ms (reagent/atom "2000")
     watch (ratom/reaction
             (when (string? @selected-id)
               (if (str/blank? @from)
                 (reset! from @selected-id)
                 (reset! to @selected-id))))]
    @watch
    [:div.form-group
     [:label
      "From"
      [:input.form-control
       {:value @from
        :on-change
        (fn [e]
          (reset! from (.. e -target -value)))}]]
     [:label
      "To"
      [:input.form-control
       {:value @to
        :on-change
        (fn [e]
          (reset! to (.. e -target -value)))}]]
     [:label
      "Step time (ms)"
      [:input.form-control
       {:value @step-ms
        :on-change
        (fn [e]
          (reset! step-ms (.. e -target -value)))}]]
     [:button.btn.btn-default
      {:on-click
       (fn [e]
         (sp/shortest-path g @from @to (js/parseInt @step-ms) (atom true)))}
      "Search"]]))

(defn show-page-rank? [g selected-id]
  [:div
   [:div.form-check
    [:label.form-check-label
     [:input.form-check-input
      {:type "checkbox"
       :checked (:show-pageranks? @g true)
       :on-change
       (fn [e]
         (swap! g assoc :show-pageranks? (.. e -target -checked)))}]
     "Scale by pagerank?"]]])

(defn collapse-reciprocal-edges? [g selected-id]
  [:div
   [:div.form-check
    [:label.form-check-label
     [:input.form-check-input
      {:type "checkbox"
       :checked (:collapse-reciprocal? @g true)
       :on-change
       (fn [e]
         (swap! g assoc :collapse-reciprocal? (.. e -target -checked)))}]
     "Collapse reciprocal links?"]]])

(defn straight-edges? [g selected-id]
  [:div
   [:div.form-check
    [:label.form-check-label
     [:input.form-check-input
      {:type "checkbox"
       :checked (:straight-edges? @g false)
       :on-change
       (fn [e]
         (swap! g assoc :straight-edges? (.. e -target -checked)))}]
     "Draw straight edges?"]]])

(defn algos [g selected-id]
  [:table.table.table-responsive
   [:tbody
    [:tr
     [:td {:style {:text-align "right"}}
      [:h4 "Reciprocal:"]]
     [:td [collapse-reciprocal-edges? g selected-id]]]
    [:tr
     [:td {:style {:text-align "right"}}
      [:h4 "Pagerank: "]]
     [:td [show-page-rank? g selected-id]]]
    [:tr
     [:td {:style {:text-align "right"}}
      [:h4 "Shortest path: "]]
     [:td [shortest-path g selected-id]]]
    [:tr
     [:td {:style {:text-align "right"}}
      [:h4 "Straight edges: "]]
     [:td [straight-edges? g selected-id]]]]])
