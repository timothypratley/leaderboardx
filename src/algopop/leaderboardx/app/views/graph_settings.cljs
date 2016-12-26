(ns algopop.leaderboardx.app.views.graph-settings
  (:require [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.views.common :as common]))

(defn graph-settings [node-types edge-types]
  [:div
   [:h3 "Node Types"]
   [:ul.list-unstyled
    (for [node-type @node-types]
      ^{:key node-type}
      [:li node-type])
    [common/add
     (fn [v]
       (db/insert! {:node/types #{v}}))]]
   [:h3 "Edge Types"]
   [:ul.list-unstyled
    (for [{:keys [db/id edge/type edge/color edge/distance]} @edge-types]
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