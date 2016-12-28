(ns algopop.leaderboardx.app.views.graph-settings
  (:require [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.views.common :as common]))

(defn graph-settings [node-types edge-types editing]
  [:div
   [:h3 "Node Types"]
   [:ul.list-unstyled
    (for [{:keys [node/type] :as row} (sort-by :node/type (vals node-types))]
      ^{:key type}
      [:li.row
       [:div.col-xs-2
        {:style {:text-align "right"}}
        [:h4 type
         [:button "x"]]]
       [:div.col-xs-10
        [:div.well
         [:ul.list-unstyled
          (for [[k v] (dissoc row :db/id :node/type)]
            [:li.row
             [:div.col-xs-2
              {:style {:font-weight "bold"
                       :text-align "right"}}
              [common/editable-string k editing]]
             [:div.col-xs-10
              [common/editable-string v editing]]])]]]])
    [common/add
     (fn [v]
       (db/insert! {:node/types #{v}}))]]
   [:h3 "Edge Types"]
   [:ul.list-unstyled
    (for [{:keys [db/id edge/type edge/color edge/distance]} (sort-by :edge/type (vals edge-types))]
      ^{:key type}
      [:li
       [:h4 type
        [:button
         {:on-click
          (fn remove-edge-type [e]
            (db/insert!
              [:db.fn/retractEntity id]))}
         "x"]]
       [:div.form-group
        [:label
         "Color"
         [:input.field
          {:default-value color}]]]
       [:div.form-group
        [:label
         "Distance"
         [:input.field
          {:default-value distance}]]]])
    [common/add
     (fn [v]
       (db/insert! {:edge/types #{{:edge/type v
                                   :edge/color "blue"
                                   :edge/distance "30"}}}))]]])