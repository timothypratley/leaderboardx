(ns algopop.leaderboardx.app.views.graph-editor
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [algopop.leaderboardx.app.db :as db]
   [algopop.leaderboardx.app.graph :as graph]
   [algopop.leaderboardx.app.seed :as seed]
   [algopop.leaderboardx.app.views.common :as common]
   [algopop.leaderboardx.app.views.d3 :as d3]
   [algopop.leaderboardx.app.views.graph-table :as table]
   [algopop.leaderboardx.app.views.toolbar :as toolbar]
   [goog.string :as gstring]
   [clojure.set :as set]
   [clojure.string :as string]
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
  (-> (.getElementById js/document "d3g")
      (.-firstChild)
      (.-innerHTML)))

(defn graph-editor-page []
  (let [g (db/get-graph)
        selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        editing (or (session/get
                     :editing)
                    (:editing (session/put! :editing (reagent/atom nil))))
        keydown-handler
        (fn a-keydown-handler [e]
          (handle-keydown e selected-id editing g))]
    (reagent/create-class
     {:display-name "graph-editor"
      :reagent-render
      (fn graph-editor []
        ;; TODO: rank g earlier
        (let [gr (graph/with-ranks @g)]
          [:div
           [toolbar/toolbar g get-svg]
           [title-editor g editing]
           [:div#d3g [d3/graph gr selected-id g editing]]
           [:div [table/table gr selected-id editing g]]]))
      :component-did-mount
      (fn graph-editor-did-mount [this]
        (.addEventListener js/document "keydown" keydown-handler))
      :component-will-unmount
      (fn graph-editor-will-unmount [this]
        (.removeEventListener js/document "keydown" keydown-handler))})))
