(ns algopop.leaderboardx.app.views.link-editor
  (:require [reagent.core :as reagent]))

(defn render-value [v]
  (cond
    (map? v) ()
    :default (pr-str v)))

(defn properties [m]
  (for [[k v] m
        :let [k (name k)]]
    [:div.form-group
     [:label.control-label.col-xs-2 {:for k} k]
     [:div.col-xs-10
      [:input.form-control {:id k
                            :type "text"
                            :default-value v}]]]))

(defn node-editor [g k]
  (let [node (get-in g [:nodes k])]
    (into [:form.form-horizontal
           [:h4 k]]
          (properties node))))

(defn leditor [g from to]
  (let [link (get-in g [:edges from to])]
    (into [:form.form-horizontal
           [:h4 from " -> " to]]
          (properties link))))

(defn selector [g selected-id]
  [:h3 "3 2 1"])

(defn link-editor [g selected-id]
  [:div
   [:form.form-horizontal
    [:div.form-group
     [:label {:for "from"}]
     [:input {:type "text"
              :name "from"
              :on-change (fn from-change [e]
                           #_(reset! from (.. e -target -value)))}]
     [:input {:type "text"
              :name "to"
              :on-change (fn to-change [e]
                           #_(reset! to (.. e -target -value)))}]]]
   (cond
     (string? @selected-id) [node-editor g @selected-id]
     (seq @selected-id) [leditor g (first @selected-id) (second @selected-id)]
     :default [selector g selected-id])])
