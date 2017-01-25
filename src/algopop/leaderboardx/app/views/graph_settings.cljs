(ns algopop.leaderboardx.app.views.graph-settings
  (:require [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.views.common :as common]))



(defn graph-settings [node-types edge-types editing]
  [:div
   [common/entity-editor
    "Node Types" node-types editing
    db/add-node-type db/remove-node-type]
   [common/entity-editor
    "Edge Types" edge-types editing
    db/add-edge-type db/remove-edge-type]])
