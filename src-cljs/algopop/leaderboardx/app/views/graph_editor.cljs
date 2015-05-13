(ns algopop.leaderboardx.app.views.graph-editor
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.seed :as seed]
            [algopop.leaderboardx.app.views.d3 :as d3]
            [goog.dom.forms :as forms]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(defonce g (reagent/atom seed/test-graph))
(def search-term (reagent/atom ""))
(def commends (reagent/atom ""))

(defn merge-left [& maps]
  (apply merge (reverse maps)))

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [form]
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap form)))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn add-em [g source targets]
  ;; TODO: move server side but keep an action list to fast UI update
  (-> g
      (update-in [:nodes] merge-left {source {}} (zipmap targets (repeat {})))
      (update-in [:edges source] merge-left (zipmap targets (repeat {})))))

(defn submit [e]
  (.preventDefault e)
  (let [{:keys [source targets]} (form-data (.-target e))]
    (swap! g add-em (string/trim source) (map string/trim (string/split targets #",")))))

(defn handle-resize [e]
  (println "RESIZE" e))

(defn handle-keydown [e]
  (case (.-keyCode e)
    46 (when @d3/selected-id
         (if (string? @d3/selected-id)
           (swap! g graph/without-node @d3/selected-id)
           (swap! g graph/without-edge @d3/selected-id)))
    (.log js/console "KEYDOWN" e)))

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
(defn input-form [gr]
  [:form {:on-submit submit}
   [:td [:label (if @search-term
                  "Add"
                  "Edit")]]
   [:td [:input {:type "text"
                 :name "source"
                 :value @search-term
                 :on-change (fn [e]
                              (let [k (.. e -target -value)]
                                (reset! search-term k)
                                (when (get-in gr [:nodes k])
                                  (reset! d3/selected-id k)
                                  ;; TODO: react instead!
                                  (reset! commends (keys (get-in gr [:edges k]))))))}]]
   [:td [:input {:type "text"
                 :name "targets"
                 :value @commends
                 :on-change (fn [e]
                              (reset! commends (.. e -target -value)))}]]
   [:td [:input {:type :submit
                 :value "↩"}]]])

(defn table [gr]
  [:table.table.table-responsive
   [:thead
    [:th "Rank"]
    [:th "Person"]
    [:th "Commends"]
    [:th "Commended by"]]
   (into
    [:tbody
     [:tr
      [input-form gr]]]
    (for [[k v] (sort-by (comp :rank val) (:nodes gr))]
      [:tr {:class (cond
                     (= k @d3/selected-id) "info"
                     (and (seq @search-term) (search [k])) "warning")
            :on-mouse-down (fn [e]
                             (reset! search-term k)
                             (reset! d3/selected-id k))}
       [:td (:rank v)]
       [:td k]
       [:td (string/join ", " (keys (get-in gr [:edges k])))]
       [:td (string/join ", " (graph/in-edges gr k))]]))])

(defn graph-editor* []
  (let [gr (graph/with-ranks @g)]
    [:div
     [d3/graph gr]
     [:div.row
      [:div.col-md-8
       [table gr]]
      [:div.col-md-4
       [toolbar gr]
       help]]]))

(defn graph-editor []
  ;; TODO: pass in session instead, and rank g earlier
  (reagent/create-class
   {:display-name "graph-editor"
    :reagent-render graph-editor*
    :component-did-mount
    (fn did-mount [this]
      (.addEventListener js/document "keydown" handle-keydown)
      (.addEventListener js/window "resize" handle-resize))
    :component-will-unmount
    (fn will-unmount [this]
      (.removeEventListener js/document "resize" handle-resize)
      (.removeEventListener js/window "keydown" handle-keydown))}))
