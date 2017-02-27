(ns algopop.leaderboardx.app.views.graph-editor
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
    [algopop.leaderboardx.app.db :as db]
    [algopop.leaderboardx.app.db-firebase :as db-firebase]
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

(defn title-editor [g editing]
  (let [title (:title @g)]
    [:div.btn-group
     [:h4
      (if (= @editing :title)
        [:form.form-inline
         {:on-submit
          (fn title-submit [e]
            (let [{:keys [new-title]} (common/form-data e)]
              (swap! g assoc :title new-title)
              (reset! editing nil)))}
         [title-input title]]
        [:span
         {:on-click
          (fn rename-click [e]
            (reset! editing :title))}
         (or title "Untitled")])]]))

(defn unselect [selected-id editing]
  (reset! selected-id nil)
  (reset! editing nil))

(defn delete-selected [selected-id editing g]
  (when-let [id @selected-id]
    (if (string? id)
      (swap! g graph/without-node id)
      (swap! g graph/without-edge id))
    (unselect selected-id editing)))

(defn maybe-delete [e selected-id editing g]
  (when-not (instance? js/HTMLInputElement (.-target e))
    (.preventDefault e)
    (delete-selected selected-id editing g)))

(defn handle-keydown [e selected-id editing g]
  (case (common/key-code-name (.-keyCode e))
    "ESC" (unselect selected-id editing)
    "DELETE" (maybe-delete e selected-id editing g)
    "BACKSPACE" (maybe-delete e selected-id editing g)
    nil))

(defn get-svg []
  (some-> (.getElementById js/document "d3g")
    (.-firstChild)
    (.-innerHTML)))

(defn graph-editor-page [{:keys [id]}]
  (let [nodes (reagent/atom {}) #_(db/watch-nodes)
        edges (reagent/atom {}) #_(db/watch-edges)
        g (reagent/atom {})
        selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        editing (or (session/get :editing)
                    (:editing (session/put! :editing (reagent/atom nil))))
        ;; TODO: find a way to get a map from datascript
        node-types (into
                     {}
                     (for [t @(db/node-types)]
                       [(:node/type t) t]))
        edge-types (into
                     {}
                     (for [t @(db/edge-types)]
                       [(:edge/type t) t]))
        next-edge-type (zipmap (keys edge-types)
                               (rest (cycle (keys edge-types))))
        selected-node-type (reagent/atom (ffirst node-types))
        selected-edge-type (reagent/atom (ffirst edge-types))
        show-settings? (reagent/atom false)
        keydown-handler
        (fn a-keydown-handler [e]
          (handle-keydown e selected-id editing g))
        callbacks
        {:shift-click-edge
         (fn shift-click-edge [{:keys [db/id edge/type]}]
           (db/insert!
             {:db/id id
              :edge/type (next-edge-type type)}))
         :shift-click-node
         (fn shift-click-node [a b]
           (db/add-edge a b))}]
    (reagent/create-class
      {:display-name "graph-editor"
       :reagent-render
       (fn graph-editor []
         [:div
          [db-firebase/watch-graph id nodes edges]
          [toolbar/toolbar g get-svg show-settings?]
          (when @show-settings?
            [:div.panel.panel-default
             [:div.panel-body
              [graph-settings/graph-settings node-types edge-types editing]]])
          [title-editor g editing]
          [:div#d3g [d3/graph node-types edge-types nodes edges selected-id editing callbacks]]
          [:div.panel.panel-default
           [:div.panel-body
            [table/table selected-id editing node-types edge-types selected-node-type selected-edge-type]]]])
       :component-did-mount
       (fn graph-editor-did-mount [this]
         (.addEventListener js/document "keydown" keydown-handler))
       :component-will-unmount
       (fn graph-editor-will-unmount [this]
         (.removeEventListener js/document "keydown" keydown-handler))})))
