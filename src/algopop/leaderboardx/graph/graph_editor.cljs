(ns algopop.leaderboardx.graph.graph-editor
  (:require
    ;;[algopop.leaderboardx.app.db :as db]
    ;;[algopop.leaderboardx.app.db-firebase :as db-firebase]
    [algopop.leaderboardx.graph.graph :as graph]
    [algopop.leaderboardx.app.views.common :as common]
    [algopop.leaderboardx.graph.graph-view :as graph-view]
    [algopop.leaderboardx.graph.graph-table :as table]
    [algopop.leaderboardx.graph.graph-settings :as graph-settings]
    [algopop.leaderboardx.app.views.toolbar :as toolbar]
    [reagent.core :as reagent]
    [reagent.ratom :refer [reaction]]
    [reagent.session :as session]
    [loom.graph :as lg]
    [algopop.leaderboardx.graph.schema :as schema]))

(defn title-editor [g]
  (let [title (:title @g)]
    [:div.btn-group
     [:h4
      [common/editable-string (or title "Untitled")
       (fn write-title [v]
         (swap! g assoc :title v))]]]))

(defn unselect [selected-id]
  (reset! selected-id nil)
  (common/blur-active-input))

(defn delete-selected [selected-id g]
  (when-let [id @selected-id]
    (if (lg/has-node? @g id)
      (swap! g graph/without-node id)
      ;; TODO: distinguish nodes/edges better in datastructure
      (let [[from to] id]
        (swap! g graph/without-edge from to)))
    (unselect selected-id)))

(defn maybe-delete [e selected-id g]
  (when-not (instance? js/HTMLInputElement (.-target e))
    (.preventDefault e)
    (delete-selected selected-id g)))

(defn handle-keydown [e selected-id g]
  (case (common/key-code-name (.-keyCode e))
    "ESC" (unselect selected-id)
    "DELETE" (maybe-delete e selected-id g)
    "BACKSPACE" (maybe-delete e selected-id g)
    nil))

(defn get-svg []
  (some-> (.getElementById js/document "d3g")
    (.-firstChild)
    (.-innerHTML)))

(defn graph-editor-page2 [g]
  (let [selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        node-types (reaction (:node-types @g))
        edge-types (reaction (:edge-types @g))
        schema (reaction {:edge/type (keys @edge-types)
                          :edge/negate :hide
                          :node/type (keys @node-types)
                          :node/shape schema/shapes})
        next-edge-type (reaction
                         (zipmap (keys @edge-types)
                                 (rest (cycle (keys @edge-types)))))
        selected-node-type (reagent/atom (ffirst @node-types))
        selected-edge-type (reagent/atom (ffirst @edge-types))
        show-settings? (reagent/atom false)
        keydown-handler
        (fn a-keydown-handler [e]
          (handle-keydown e selected-id g))
        callbacks
        {:shift-click-edge
         (fn shift-click-edge [edge-id {:keys [edge/type]}]
           (swap! g graph/update-edge edge-id
                  :edge/type (@next-edge-type type)))

         ;; TODO: omg don't pass shapes, but don't want to look up either
         :shift-click-node
         (fn shift-click-node [a b]
           (if (= a b)
             (swap! g graph/update-attr a :node/shape schema/next-shape)
             (when-not (vector? a)
               (swap! g graph/with-edge a b @selected-edge-type))))

         :double-click-node
         (fn double-click-node [node-id])}]

    ;; TODO: I think all the derefing in here defeats the purpose of reactions
    (reagent/create-class
      {:display-name "graph-editor"
       :reagent-render
       (fn graph-editor []
         [:div
          [toolbar/toolbar g get-svg show-settings? selected-id]
          (when @show-settings?
            [:div.panel.panel-default
             [:div.panel-body
              [graph-settings/graph-settings g @node-types @edge-types]]])
          [title-editor g]
          (when @selected-id
            [:div.panel.panel-default.pull-left
             {:style {:position "absolute"
                      :width "25%"}}
             ;; TODO: when this covers a node, it causes the node to be grabbed
             [table/attribute-editor g selected-id schema]])
          [:div#d3g
           [graph-view/graph-view g node-types edge-types selected-id selected-edge-type callbacks]]
          [:div.panel.panel-default
           [:div.panel-body
            [table/table g selected-id node-types edge-types selected-node-type selected-edge-type callbacks]]]])
       :component-did-mount
       (fn graph-editor-did-mount [this]
         (.addEventListener js/document "keydown" keydown-handler))
       :component-will-unmount
       (fn graph-editor-will-unmount [this]
         (.removeEventListener js/document "keydown" keydown-handler))})))

(defonce g
  (reagent/atom (graph/create)))

(defn graph-editor-page [{:keys [id]}]
  [graph-editor-page2 g])
