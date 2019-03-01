(ns algopop.leaderboardx.graph.graph-settings
  (:require
    [algopop.leaderboardx.app.views.common :as common]
    [algopop.leaderboardx.graph.schema :as schema]))

(defn graph-settings [g node-types edge-types]
  [:div
   [common/entity-editor
    "Node Types"
    node-types
    ;; TODO: maybe merge instead
    ;; TODO: these all look the same except the parent key?
    ;; TODO: DRY ME
    #(swap! g update :node-types assoc %1 %2)
    #(swap! g update :node-types dissoc %1)
    #(swap! g update-in [:node-types %1] assoc (keyword "node" %2) %3)
    #(swap! g update-in [:node-types %1] dissoc %2)
    {:node/shape schema/shapes}]
   [common/entity-editor
    "Edge Types"
    edge-types
    #(swap! g update :edge-types assoc %1 %2)
    #(swap! g update :edge-types dissoc %1)
    #(swap! g update-in [:edge-types %1] assoc (keyword "edge" %2) %3)
    #(swap! g update-in [:edge-types %1] dissoc %2)
    {:edge/negate [false true]}]])
