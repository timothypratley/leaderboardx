(ns algopop.leaderboardx.app.views.graph-editor
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.seed :as seed]
            [algopop.leaderboardx.app.views.d3 :as d3]
            [algopop.leaderboardx.app.views.toolbar :as toolbar]
            [goog.dom.forms :as forms]
            [clojure.string :as string]
            [reagent.core :as reagent])
  (:import [goog.events KeyCodes]))

(defonce g (reagent/atom (seed/rand-graph)))

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [e]
  (.preventDefault e)
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn submit-add-node-and-edges [e]
  (let [{:keys [source targets]} (form-data e)
        source (string/trim source)
        targets (map string/trim (string/split targets #","))]
    (swap! g graph/add-edges source targets)))

(defn unselect [selected-id]
  (reset! selected-id nil))

(defn delete-selected [selected-id]
  (when-let [id @selected-id]
    (if (string? id)
      (swap! g graph/without-node id)
      (swap! g graph/without-edge id))
    (unselect selected-id)))

(defn key-match [k e]
  (= (aget KeyCodes k) (.-keyCode e)))

(defn handle-keydown [e selected-id]
  (condp key-match e
    "ESC" (unselect selected-id)
    "DELETE" (when-not (instance? js/HTMLInputElement (.-target e))
               (delete-selected selected-id))
    nil))

(defn input-row [gr search-term commends selected-id]
  [:tr
   [:td [:label "Add"]]
   [:td [:input {:type "text"
                 :name "source"
                 :style {:width "100%"}
                 ;; TODO: reset search-term and commends when selection made
                 :value @search-term
                 :on-change (fn source-on-change [e]
                              (let [k (.. e -target -value)]
                                (reset! search-term k)
                                (when (get-in gr [:nodes k])
                                  (reset! selected-id k)
                                  ;; TODO: react instead!
                                  (reset! commends (keys (get-in gr [:edges k]))))))}]]
   [:td [:input {:type "text"
                 :name "targets"
                 :style {:width "100%"}
                 :value @commends
                 :on-change (fn targets-on-change [e]
                              (reset! commends (.. e -target -value)))}]]])

(defn focus-append [this]
  (doto (.getDOMNode this)
    (.focus)
    (.setSelectionRange 100000 100000)))

(defn finish-edit [editing e]
  (reset! editing nil))

(defn node-input [k editing]
  (reagent/create-class
   {:display-name "node-input"
    :component-did-mount focus-append
    :reagent-render
    (fn node-input-render [k editing]
      [:input {:type "text"
               :name "new-name"
               :default-value k
               :on-blur (fn node-input-blur [e]
                          (reset! editing nil))}])}))

(defn rename-node [k editing]
  [:form {:on-submit (fn rename-node-submit [e]
                       (let [{:keys [new-name]} (form-data e)]
                         (swap! g graph/rename-node k new-name))
                       (reset! editing nil))}
   [node-input k editing]])

(defn edge-input [edges editing]
  (reagent/create-class
   {:display-name "edge-input"
    :component-did-mount focus-append
    :reagent-render
    (fn edge-input-render [edges editing]
      [:input {:type "text"
               :name "targets"
               :default-value edges
               :on-blur (fn edge-input-blur [e]
                          (reset! editing nil))}])}))

(defn edit-edges [k edges editing]
  [:form {:on-submit (fn edit-edges-submit [e]
                       (submit-add-node-and-edges e)
                       (reset! editing nil))}
   [:input {:type "hidden"
            :name "source"
            :value k}]
   [edge-input edges editing]])

(defn table [gr selected-id]
  (let [search-term (reagent/atom "")
        commends (reagent/atom "")
        editing (reagent/atom nil)]
    (fn a-table [gr]
      [:form {:on-submit submit-add-node-and-edges}
       [:table.table.table-responsive
        [:thead
         [:th "Rank"]
         [:th "Person"]
         [:th "Commends"]
         [:th "Commended by"]]
        (into
         [:tbody
          [input-row gr search-term commends selected-id]]
         (for [[k v] (sort-by (comp :rank val) (:nodes gr))
               :let [selected? (= k @selected-id)
                     match? (and (seq @search-term) (.startsWith k @search-term))
                     edges (string/join ", " (keys (get-in gr [:edges k])))]]
           [:tr {:class (cond
                          selected? "info"
                          match? "warning")
                 :on-mouse-down (fn table-mouse-down [e]
                                  (reset! search-term k)
                                  (reset! selected-id k))}
            [:td (:rank v)]
            [:td {:on-mouse-down (fn node-name-mouse-down [e]
                                   (when (= k @selected-id)
                                     (reset! editing :node)))}
             (if (and selected? (#{:node} @editing))
               [rename-node k editing]
               k)]
            [:td {:on-mouse-down (fn edges-mouse-down [e]
                                   (when (= k @selected-id)
                                     (reset! editing :edges)))}
             (if (and selected? (#{:edges} @editing))
               [edit-edges k edges editing]
               edges)]
            [:td (string/join ", " (graph/in-edges gr k))]]))]])))

(defn graph-editor-page []
  ;; TODO: pass in session instead, and rank g earlier
  (let [selected-id (reagent/atom nil)
        keydown-handler (fn a-keydown-handler [e]
                          (handle-keydown e selected-id))]
    (reagent/create-class
     {:display-name "graph-editor"
      :reagent-render
      (fn graph-editor []
        (let [gr (graph/with-ranks @g)
              this (reagent/current-component)
              get-svg (fn a-get-svg []
                        (-> this
                            (.getDOMNode)
                            (.-firstChild)
                            (.-firstChild)
                            (.-innerHTML)))]
          [:div
           [d3/graph gr selected-id]
           [toolbar/toolbar g get-svg]
           [table gr selected-id]]))
      :component-did-mount
      (fn graph-editor-did-mount [this]
        (.addEventListener js/document "keydown" keydown-handler))
      :component-will-unmount
      (fn graph-editor-will-unmount [this]
        (.removeEventListener js/document "keydown" keydown-handler))})))
