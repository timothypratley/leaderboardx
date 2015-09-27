(ns algopop.leaderboardx.app.views.coach
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.db :as db]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(defonce goals
  (reagent/atom {1 "Increase passes per attack on goal from 3 to 4."
                 2 "Run faster - team relay 4min."}))

(defn goal-input [goal editing-id]
  (reagent/create-class
   {:display-name "node-input"
    :component-did-mount common/focus-append
    :reagent-render
    (fn a-goal-input [goal editing-id]
      [:input {:type "text"
               :name "new-goal"
               :style {:width "100%"}
               :default-value goal
               :on-blur (fn node-input-blur [e]
                          (reset! editing-id nil))}])}))

(defn goal-form [goal editing-id]
  [:form
   {:on-submit (fn rename-node-submit [e]
                 (let [new-goal (string/trim (:new-goal (common/form-data e)))]
                   (if (seq new-goal)
                     (swap! goals assoc @editing-id new-goal)
                     (swap! goals dissoc @editing-id)))
                 (reset! editing-id nil))}
   [goal-input goal editing-id]])

;; Team goals
(defn team-goals []
  (let [editing-id (reagent/atom nil)]
    (fn a-team-goals []
      [:div
       (into [:ul]
             (for [[goal-id goal] @goals]
               [:li (if (= goal-id @editing-id)
                      [goal-form goal editing-id]
                      [:span {:on-click (fn goal-click [e]
                                          (reset! editing-id goal-id))}
                       goal])]))
       [:button.btn.btn-default
        {:on-click (fn on-add-goal-click [e]
                     (swap! goals assoc (rand-int 1000) "New goal"))}
        "Add goal"]])))

;; Recuring assessments
(defn assessments []
  [:div
   [:p "Sprint time " [:a {:href "#"} "Daniel"]]])

;; Upcoming checkpoints
(defn checkpoints []
  [:div
   [:p "Quarter finals coming up!"]])

;; Quick links to team members
(defn quick-links [nodes]
  [:div
   (into [:p]
         (interpose
          " - "
          (for [[k v] (sort nodes)]
            [:a {:href "#"} k])))])

(defn coach-view []
  (let [nodes (db/watch-nodes)]
    [:div
     [:h2 "Team goals:"]
     [team-goals]
     [:h2 "Assessments"]
     [assessments]
     [:h2 "Checkpoints"]
     [checkpoints]
     [:h2 "Quick Links"]
     [quick-links @nodes]]))
