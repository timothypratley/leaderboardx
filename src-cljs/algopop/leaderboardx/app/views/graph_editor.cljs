(ns algopop.leaderboardx.app.views.graph-editor
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.seed :as seed]
            [algopop.leaderboardx.app.views.d3 :as d3]
            [algopop.leaderboardx.app.views.link-editor :as link-editor]
            [algopop.leaderboardx.app.views.toolbar :as toolbar]
            [goog.dom.forms :as forms]
            [goog.string :as gstring]
            [clojure.set :as set]
            [clojure.string :as string]
            [reagent.core :as reagent])
  (:import [goog.events KeyCodes]))

(defonce g (reagent/atom seed/example))

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [e]
  (.preventDefault e)
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn submit-add-node-and-edges [e selected-id]
  (let [{:keys [source outs ins]} (form-data e)
        source (string/trim source)
        outs (map string/trim (string/split outs #"[,;]"))
        ins (map string/trim (string/split ins #"[,;]"))]
    (swap! g graph/replace-edges source outs ins)
    (reset! selected-id source)))

(defn unselect [selected-id]
  (reset! selected-id nil))

(defn delete-selected [selected-id]
  (when-let [id @selected-id]
    (if (string? id)
      (swap! g graph/without-node id)
      (swap! g graph/without-edge id))
    (unselect selected-id)))

(defn maybe-delete [e selected-id]
  (when-not (instance? js/HTMLInputElement (.-target e))
    (.preventDefault e)
    (delete-selected selected-id)))

(def codename
  (set/map-invert (js->clj KeyCodes)))

(defn handle-keydown [e selected-id]
  (case (codename (.-keyCode e))
    "ESC" (unselect selected-id)
    "DELETE" (maybe-delete e selected-id)
    "BACKSPACE" (maybe-delete e selected-id)
    nil))

(defn humanize-edges [edges]
  (string/join ", " (sort edges)))

(defn input-row [gr search-term selected-id]
  (let [outs (reagent/atom "")
        ins (reagent/atom "")]
    (fn an-input-row [gr search-term selected-id]
      (remove-watch selected-id :selection)
      (add-watch selected-id :selection
                 (fn selection-watcher [k r a b]
                   (reset! search-term b)
                   (reset! outs (humanize-edges (keys (get-in gr [:edges b]))))
                   (reset! ins (humanize-edges (keys (graph/in-edges gr b))))))
      [:tr
       [:td [:input.btn.btn-default.btn-xs
             {:type "submit"
              :value "Add"}]]
       [:td [:input#from
             {:type "text"
              :name "source"
              :style {:width "100%"}
              :value @search-term
              :on-change (fn source-on-change [e]
                           (let [k (.. e -target -value)]
                             (reset! search-term k)
                             (when (get-in gr [:nodes k])
                               (reset! selected-id k))))}]]
       [:td [:input
             {:type "text"
              :name "outs"
              :style {:width "100%"}
              :value @outs
              :on-change (fn targets-on-change [e]
                           (reset! outs (.. e -target -value)))}]]
       [:td [:input
             {:type "text"
              :name "ins"
              :style {:width "100%"}
              :value @ins
              :on-change (fn targets-on-change [e]
                           (reset! ins (.. e -target -value)))}]]])))

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
               :style {:width "100%"}
               :default-value k
               :on-blur (fn node-input-blur [e]
                          (reset! editing nil))}])}))

(defn rename-node [k selected-id editing]
  [:form {:on-submit (fn rename-node-submit [e]
                       (let [{:keys [new-name]} (form-data e)]
                         (swap! g graph/rename-node k new-name)
                         (reset! selected-id new-name))
                       (reset! editing nil))}
   [node-input k editing]])

(defn edge-input [edges editing]
  (reagent/create-class
   {:display-name "edge-input"
    :component-did-mount focus-append
    :reagent-render
    (fn edge-input-render [edges editing]
      [:input {:type "text"
               :name (if (#{:ins} @editing)
                       "ins"
                       "outs")
               :style {:width "100%"}
               :default-value edges
               :on-blur (fn edge-input-blur [e]
                          (reset! editing nil))}])}))

(defn edit-edges [k outs ins selected-id editing]
  [:form {:on-submit (fn edit-edges-submit [e]
                       (submit-add-node-and-edges e selected-id)
                       (reset! editing nil))}
   [:input {:type "hidden"
            :name "source"
            :value k}]
   (if (#{:ins} @editing)
     [:input {:type "hidden"
              :name "outs"
              :value outs}]
     [:input {:type "hidden"
              :name "ins"
              :value ins}])
   [edge-input
    (if (#{:ins} @editing)
      ins
      outs)
    editing]])

(defn table [gr selected-id]
  (let [search-term (reagent/atom "")
        editing (reagent/atom nil)]
    (fn a-table [gr]
      (conj
       (if @editing
         [:div]
         [:form {:on-submit (fn add-node-submit [e]
                              (submit-add-node-and-edges e selected-id)
                              (doto (.getElementById js/document "from")
                                (.focus)
                                (.setSelectionRange 0 100000)))}])
       [:table.table.table-responsive
        [:thead
         [:th "Rank"]
         [:th "Person"]
         [:th "Endorses"]
         [:th "Endorsed by"]]
        (into
         [:tbody
          [input-row gr search-term selected-id]]
         (for [[k {:keys [rank]}] (sort-by (comp :rank val) (:nodes gr))
               :let [selected? (= k @selected-id)
                     match? (and (seq @search-term) (gstring/startsWith k @search-term))
                     outs (humanize-edges (keys (get-in gr [:edges k])))
                     ins (humanize-edges (keys (graph/in-edges gr k)))]]
           [:tr {:class (cond selected? "info"
                              match? "warning")
                 :style {:cursor "pointer"}
                 :on-click (fn table-row-click [e]
                             (reset! selected-id k))}
            [:td rank]
            [:td {:style {:cursor "pointer"}
                  :on-click (fn node-name-click [e]
                              (when (= k @selected-id)
                                (reset! editing :node)))}
             (if (and selected? (#{:node} @editing))
               [rename-node k selected-id editing]
               k)]
            [:td {:style {:cursor "pointer"}
                  :on-click (fn outs-click [e]
                              (when (= k @selected-id)
                                (reset! editing :outs)))}
             (if (and selected? (#{:outs} @editing))
               [edit-edges k outs ins selected-id editing]
               outs)]
            [:td {:style {:cursor "pointer"}
                  :on-click (fn ins-click [e]
                              (when (= k @selected-id)
                                (reset! editing :ins)))}
             (if (and selected? (#{:ins} @editing))
               [edit-edges k outs ins selected-id editing]
               ins)]]))]))))

(defn title-input [title editing]
  (reagent/create-class
   {:display-name "title-input"
    :component-did-mount focus-append
    :reagent-render
    (fn title-input-render [title editing]
      [:input {:type "text"
               :name "new-title"
               :style {:width "550px"}
               :default-value title
               :on-blur (fn title-input-blur [e]
                          (reset! editing nil))}])}))

(defn title-editor [g]
  (let [editing (reagent/atom nil)]
    (fn a-rename-button [g]
      (let [title (:title @g)]
        [:div.btn-group
         [:h4
          (if @editing
            [:form.form-inline
             {:on-submit (fn title-submit [e]
                           (let [{:keys [new-title]} (form-data e)]
                             (swap! g assoc :title new-title)
                             (reset! editing nil)))}
             [title-input title editing]]
            [:span {:on-click (fn rename-click [e]
                                (reset! editing :title))}
             (or title "Untitled")])]]))))

(defn get-svg []
  (-> (.getElementById js/document "d3g")
      (.-firstChild)
      (.-innerHTML)))

(defn graph-editor-page []
  ;; TODO: pass in session instead, and rank g earlier
  (let [selected-id (reagent/atom nil)
        keydown-handler (fn a-keydown-handler [e]
                          (handle-keydown e selected-id))]
    (reagent/create-class
     {:display-name "graph-editor"
      :reagent-render
      (fn graph-editor []
        (let [gr (graph/with-ranks @g)]
          [:div
           [toolbar/toolbar g get-svg]
           [title-editor g]
           [:div#d3g [d3/graph gr selected-id]]
           [:div [table gr selected-id]]]))
      :component-did-mount
      (fn graph-editor-did-mount [this]
        (.addEventListener js/document "keydown" keydown-handler))
      :component-will-unmount
      (fn graph-editor-will-unmount [this]
        (.removeEventListener js/document "keydown" keydown-handler))})))
