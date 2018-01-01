(ns algopop.leaderboardx.app.views.graph-editor
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
    ;;[algopop.leaderboardx.app.db :as db]
    ;;[algopop.leaderboardx.app.db-firebase :as db-firebase]
    [algopop.leaderboardx.app.graph :as graph]
    [algopop.leaderboardx.app.views.common :as common]
    [algopop.leaderboardx.app.views.d3 :as d3]
    [algopop.leaderboardx.app.views.graph-table :as table]
    [algopop.leaderboardx.app.views.graph-settings :as graph-settings]
    [algopop.leaderboardx.app.views.toolbar :as toolbar]
    [reagent.core :as reagent]
    [reagent.session :as session]))

(defn title-input [title]
  (reagent/create-class
    {:display-name "title-input"
     :component-did-mount common/focus-append
     :reagent-render
     (fn title-input-render [title]
       [:input {:type "text"
                :name "new-title"
                :style {:width "550px"}
                :default-value title}])}))

(defn title-editor [g]
  (let [title (:title @g)]
    [:div.btn-group
     [:h4
      [common/editable-string (or title "Untitled") nil
       (fn title-submit [v]
         (swap! g assoc :title v))]]]))

(defn unselect [selected-id]
  (reset! selected-id nil)
  (common/blur-active-input))

(defn delete-selected [selected-id g]
  (when-let [id @selected-id]
    (if (string? id)
      (swap! g graph/without-node id)
      (swap! g graph/without-edge id))
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
        ;; TODO: find a way to get a map from datascript
        node-types (reagent/atom {"person" {}})
        edge-types (reagent/atom {"likes" {:edge/color "#9ecae1"
                                           :edge/dasharray "1"
                                           :edge/distance 30
                                           :weight 1
                                           :negate false}
                                  "dislikes" {:edge/color "red"
                                              :edge/dasharray "1"
                                              :edge/distance 100
                                              :weight 1
                                              :negate true}})

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
         (fn shift-click-edge [{:keys [edge/from edge/to edge/type]}]
           (swap! g graph/update-edge from to
                  {:edge/type (@next-edge-type type)}))
         :shift-click-node
         (fn shift-click-node [a b]
           (swap! g graph/add-edge a b @selected-edge-type))}]
    (reagent/create-class
      {:display-name "graph-editor"
       :reagent-render
       (fn graph-editor []
         [:div
          [toolbar/toolbar g get-svg show-settings?]
          (when @show-settings?
            [:div.panel.panel-default
             [:div.panel-body
              [graph-settings/graph-settings node-types edge-types]]])
          [title-editor g]
          [:div#d3g
           [d3/graph g node-types edge-types selected-id selected-edge-type callbacks]]
          [:div.panel.panel-default
           [:div.panel-body
            [table/table g selected-id node-types edge-types selected-node-type selected-edge-type]]]])
       :component-did-mount
       (fn graph-editor-did-mount [this]
         (.addEventListener js/document "keydown" keydown-handler))
       :component-will-unmount
       (fn graph-editor-will-unmount [this]
         (.removeEventListener js/document "keydown" keydown-handler))})))

(defn graph-editor-page [{:keys [id]}]
  (let [g (reagent/atom {:nodes {}
                         :edges {}})]
    [graph-editor-page2 g]))
