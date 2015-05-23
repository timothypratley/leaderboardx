(ns algopop.leaderboardx.app.views.graph-editor
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.io.dot :as dot]
            [algopop.leaderboardx.app.io.csv :as csv]
            [algopop.leaderboardx.app.seed :as seed]
            [algopop.leaderboardx.app.views.d3 :as d3]
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

;; TODO: don't take empty str, etc
(defn submit-add-node-and-edges [e]
  (let [{:keys [source targets]} (form-data e)
        source (string/trim source)
        targets (map string/trim (string/split targets #","))]
    (swap! g graph/add-edges source targets)))

(defn unselect []
  (reset! d3/selected-id nil))

(defn delete-selected []
  (when @d3/selected-id
    (if (string? @d3/selected-id)
      (swap! g graph/without-node @d3/selected-id)
      (swap! g graph/without-edge @d3/selected-id))
    (unselect)))

(defn key-match [k e]
  (= (.-keyCode e) (aget KeyCodes k)))

(defn handle-keydown [e]
  (condp key-match e
    "ESC" (unselect)
    "DELETE" (when-not (instance? js/HTMLInputElement (.-target e))
                 (delete-selected))
    nil))

(defn help []
  (let [show-help (reagent/atom false)]
    (fn a-help []
      [:div.pull-right
       [:span.btn.btn-default.pull-right
        {:on-click (fn help-click [e]
                     (swap! show-help not))}
        [:span.glyphicon.glyphicon-question-sign {:aria-hidden "true"}]]
       (when @show-help
         [:div.panel.panel-default
          {:on-click (fn help-panel-click [e]
                       (swap! show-help not))}
          [:div.panel-body
           [:ul.list-unstyled
            [:li "Enter a node name and press ENTER to add it."]
            [:li "Enter a comma separated list of nodes to link to and press ENTER to add them."]
            [:li "Select a node or edge by mouse clicking it and press DEL to delete it."]
            [:li "Drag nodes or edges around by click hold and move."]
            [:li "Double click to unpin nodes and edges."]]]])])))

(defn toolbar [gr]
  [:div
   [:span.btn.btn-default
    {:on-click (fn clear-click [e]
                 (reset! g {:nodes {"root" {}}
                            :edges {"root" {}}}))}
    "Clear"]
   [:span.btn.btn-default
    {:on-click (fn random-click [e]
                 (reset! g (seed/rand-graph)))}
    "Random"]
   [:span.btn.btn-default.btn-file
    "Import CSV"
    [:input
     {:type "file"
      :name "import"
      :accept "text/csv"}]]
   [:span.btn.btn-default
    {:on-click (fn import-graphviz-click [e]
                 (doto
                     (reset! g (dot/read-graph dot/dot))
                   (prn "GRAPH")))}
    "Import Graphviz"]
   ;; TODO: make these calculate when you click not before
   [:a.btn.btn-default
    {:href (js/encodeURI (str "data:text/csv;charset=utf-8," (csv/write-graph gr)))
     :download "graph.csv"}
    "Export CSV"]
   [:a.btn.btn-default
    {:href (js/encodeURI (str "data:text/dot;charset=utf-8," (dot/write-graph gr)))
     :download "graph.dot"}
    "Export Graphviz"]
   [:a.btn.btn-default
    {:href (js/encodeURI (str "data:image/svg+xml;base64," (d3/graph gr)))
     :download "graph.dot"}
    "Export SVG"]
   [help]])

(defn input-row [gr search-term commends]
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
                                  (reset! d3/selected-id k)
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

(defn table [gr]
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
          [input-row gr search-term commends]]
         (for [[k v] (sort-by (comp :rank val) (:nodes gr))
               :let [selected? (= k @d3/selected-id)
                     match? (and (seq @search-term) (.startsWith k @search-term))
                     edges (string/join ", " (keys (get-in gr [:edges k])))]]
           [:tr {:class (cond
                          selected? "info"
                          match? "warning")
                 :on-mouse-down (fn table-mouse-down [e]
                                  (reset! search-term k)
                                  (reset! d3/selected-id k))}
            [:td (:rank v)]
            [:td {:on-mouse-down (fn node-name-mouse-down [e]
                                   (when (= k @d3/selected-id)
                                     (reset! editing :node)))}
             (if (and selected? (#{:node} @editing))
               [rename-node k editing]
               k)]
            [:td {:on-mouse-down (fn edges-mouse-down [e]
                                   (when (= k @d3/selected-id)
                                     (reset! editing :edges)))}
             (if (and selected? (#{:edges} @editing))
               [edit-edges k edges editing]
               edges)]
            [:td (string/join ", " (graph/in-edges gr k))]]))]])))

(defn graph-editor []
  (let [gr (graph/with-ranks @g)]
    [:div
     [d3/graph gr]
     [toolbar gr]
     [table gr]]))

(defn graph-editor-page []
  ;; TODO: pass in session instead, and rank g earlier
  (reagent/create-class
   {:display-name "graph-editor"
    :reagent-render graph-editor
    :component-did-mount
    (fn graph-editor-did-mount [this]
      (.addEventListener js/document "keydown" handle-keydown))
    :component-will-unmount
    (fn graph-editor-will-unmount [this]
      (.removeEventListener js/document "keydown" handle-keydown))}))
