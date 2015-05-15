(ns algopop.leaderboardx.app.views.graph-editor
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.seed :as seed]
            [algopop.leaderboardx.app.views.d3 :as d3]
            [goog.dom.forms :as forms]
            [clojure.string :as string]
            [reagent.core :as reagent])
  (:import [goog.events KeyCodes]))

(defonce g (reagent/atom seed/test-graph))
(def search-term (reagent/atom ""))
(def commends (reagent/atom ""))

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [form]
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap form)))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

;; TODO: don't take empty str, etc
(defn submit [e]
  (.preventDefault e)
  (let [{:keys [source targets]} (form-data (.-target e))
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
    "DELETE" (delete-selected)
    nil))

(defn search [[k v]]
  (or (empty? @search-term)
      (.startsWith k @search-term)))

(defn help []
  [:ul.list-unstyled
   [:li "Enter a node name and press ENTER to add it."]
   [:li "Enter a comma separated list of nodes to link to and press ENTER to add them."]
   [:li "Select a node or edge by mouse clicking it and press DEL to delete it."]
   [:li "Drag nodes or edges around by click hold and move."]
   [:li "Double click to unpin nodes and edges."]])

(defn toolbar [gr]
  [:div
   [:span.btn.btn-default.btn-file
    "Import"
    [:input
     {:type "file"
      :name "import"
      :accept "text/csv"}]]
   [:a.btn.btn-default
    {:href (js/encodeURI (str "data:text/csv;charset=utf-8," (pr-str gr)))
     :download "graph.csv"}
    "Export"]])

;; TODO: pass args instead of globals
;; TODO: form has to wrap table??
(defn input-form [gr]
  [:tr
   [:td [:label (if @search-term
                  "Add"
                  "Edit")]]
   [:td [:input {:type "text"
                 :name "source"
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
                 :value @commends
                 :on-change (fn targets-on-change [e]
                              (reset! commends (.. e -target -value)))}]]
   [:td [:input {:type :submit
                 :value "↩"}]]])

(defn table [gr]
  [:form {:on-submit submit}
   [:table.table.table-responsive
    [:thead
     [:th "Rank"]
     [:th "Person"]
     [:th "Commends"]
     [:th "Commended by"]]
    (into
     [:tbody
      [input-form gr]]
     (for [[k v] (sort-by (comp :rank val) (:nodes gr))]
       [:tr {:class (cond
                      (= k @d3/selected-id) "info"
                      (and (seq @search-term) (search [k])) "warning")
             :on-mouse-down (fn table-mouse-down [e]
                              (reset! search-term k)
                              (reset! d3/selected-id k))}
        [:td (:rank v)]
        [:td k]
        [:td (string/join ", " (keys (get-in gr [:edges k])))]
        [:td (string/join ", " (graph/in-edges gr k))]]))]])

(defn graph-editor []
  (let [gr (graph/with-ranks @g)]
    [:div
     [d3/graph gr]
     [:div.row
      [:div.col-md-8
       [table gr]]
      [:div.col-md-4
       [toolbar gr]
       [help]]]]))

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
