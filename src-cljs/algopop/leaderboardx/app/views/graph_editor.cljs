(ns algopop.leaderboardx.app.views.graph-editor
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [algopop.leaderboardx.app.graph :as graph]
   [algopop.leaderboardx.app.seed :as seed]
   [algopop.leaderboardx.app.views.common :as common]
   [algopop.leaderboardx.app.views.d3 :as d3]
   [algopop.leaderboardx.app.views.link-editor :as link-editor]
   [algopop.leaderboardx.app.views.toolbar :as toolbar]
   [goog.string :as gstring]
   [clojure.set :as set]
   [clojure.string :as string]
   [reagent.core :as reagent]
   [reagent.session :as session])
  (:import [goog.events KeyCodes]))

(defn submit-add-node-and-edges [e selected-id g]
  (let [{:keys [source outs ins]} (common/form-data e)
        source (string/trim source)
        outs (map string/trim (string/split outs #"[,;]"))
        ins (map string/trim (string/split ins #"[,;]"))]
    (swap! g graph/replace-edges source outs ins)
    (reset! selected-id source)))

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

(def codename
  ;; TODO: advanced mode (set/map-invert (js->clj KeyCodes))
  {27 "ESC"
   46 "DELETE"
   8 "BACKSPACE"})

(defn handle-keydown [e selected-id editing g]
  (case (codename (.-keyCode e))
    "ESC" (unselect selected-id editing)
    "DELETE" (maybe-delete e selected-id editing g)
    "BACKSPACE" (maybe-delete e selected-id editing g)
    nil))

(defn humanize-edges [edges]
  (string/join ", " (sort edges)))

(defn input-row [gr search-term selected-id editing]
  (let [outs (reagent/atom "")
        ins (reagent/atom "")]
    (fn an-input-row [gr search-term selected-id]
      (remove-watch selected-id :selection)
      (add-watch selected-id :selection
                 (fn selection-watcher [k r a b]
                   (reset! search-term b)
                   (reset! outs (humanize-edges (keys (get-in gr [:edges b]))))
                   (reset! ins (humanize-edges (keys (graph/in-edges gr b))))))
      (if (= @editing :add)
        [:tr
         [:td
          [:input.btn.btn-default.btn-xs
           {:type "submit"
            :value "Add"}]]
         [:td
          [common/focus-append-input
           {:id "from"
            :name "source"
            :on-change
            (fn source-on-change [e]
              (let [k (.. e -target -value)]
                (reset! search-term k)
                (when (get-in gr [:nodes k])
                  (reset! selected-id k))))}]]
         [:td
          [:input
           {:type "text"
            :name "outs"
            :style {:width "100%"}
            :value @outs
            :on-change
            (fn targets-on-change [e]
              (reset! outs (.. e -target -value)))}]]
         [:td
          [:input
           {:type "text"
            :name "ins"
            :style {:width "100%"}
            :value @ins
            :on-change
            (fn targets-on-change [e]
              (reset! ins (.. e -target -value)))}]]]
        [:tr
         [:td
          [:input.btn.btn-default.btn-xs
           {:type "submit"
            :on-click
            (fn add-click [e]
              (reset! editing :add))
            :value "Add"}]]
         [:td
          {:on-focus
           (fn add-focus [e]
             (reset! editing :add))}
          [:input {:type "text"
                   :style {:width "100%"}}]]]))))

(defn rename-node [k selected-id editing g]
  [:form
   {:on-submit
    (fn rename-node-submit [e]
      (let [{:keys [text]} (common/form-data e)]
        (swap! g graph/rename-node k text)
        (reset! selected-id text))
      (reset! editing nil))}
   [common/focus-append-input
    {:default-value k}]])

(defn edit-edges [k outs ins selected-id editing g]
  [:form
   {:on-submit
    (fn edit-edges-submit [e]
      (submit-add-node-and-edges e selected-id g)
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
   [common/focus-append-input
    (if (= @editing :ins)
      {:name "ins"
       :default-value ins}
      {:name "outs"
       :default-value outs})]])

(def node-types
  ["Person"
   "Assessment"])

(def link-types
  ["Endorses"
   "Owns"])

;; TODO: use re-com
(defn select-type [types editing]
  (let [current-type (reagent/atom (first types))]
    (fn a-select-type []
      (if (= @editing types)
        [:th
         (into
          [:select
           {:on-change
            (fn selection [e]
              (when-let [idx (.-selectedIndex (.-target e))]
                (reset! current-type (types idx))
                (reset! editing nil)))}]
          (for [t types]
            [:option t]))]
        [:th
         {:on-click
          (fn type-click [e]
            (reset! editing types)
            nil)}
         @current-type]))))

(defn table [gr selected-id editing g]
  (let [search-term (reagent/atom "")]
    (fn a-table [gr]
      (conj
       (if (= @editing :add)
         [:form
          {:on-submit
           (fn add-node-submit [e]
             (submit-add-node-and-edges e selected-id g)
             (doto (.getElementById js/document "from")
               (.focus)
               (.setSelectionRange 0 100000)))}]
         [:div])
       [:table.table.table-responsive
        [:thead
         [:th "Rank"]
         [select-type node-types editing]
         [select-type link-types editing]
         [:th "From"]]
        (into
         [:tbody
          [input-row gr search-term selected-id editing]]
         (for [[k {:keys [rank]}] (sort-by (comp :rank val) (:nodes gr))
               :let [selected? (= k @selected-id)
                     match? (and (seq @search-term) (gstring/startsWith k @search-term))
                     outs (humanize-edges (keys (get-in gr [:edges k])))
                     ins (humanize-edges (keys (graph/in-edges gr k)))]]
           [:tr
            {:class (cond selected? "info"
                          match? "warning")
             :style {:cursor "pointer"}
             :on-click
             (fn table-row-click [e]
               (reset! selected-id k))}
            [:td rank]
            [:td.editable
             {:style {:cursor "pointer"}
              :on-click
              (fn node-name-click [e]
                (reset! editing :node))}
             (if (and selected? (#{:node} @editing))
               [rename-node k selected-id editing g]
               [:span
                k
                [:span.glyphicon.glyphicon-pencil.pull-right.edit]])]
            [:td.editable
             {:style {:cursor "pointer"}
              :on-click
              (fn outs-click [e]
                (reset! editing :outs))}
             (if (and selected? (#{:outs} @editing))
               [edit-edges k outs ins selected-id editing g]
               [:span
                outs
                [:span.glyphicon.glyphicon-pencil.pull-right.edit]])]
            [:td.editable
             {:style {:cursor "pointer"}
              :on-click
              (fn ins-click [e]
                (reset! editing :ins))}
             (if (and selected? (#{:ins} @editing))
               [edit-edges k outs ins selected-id editing]
               [:span
                ins
                [:span.glyphicon.glyphicon-pencil.pull-right.edit]])]]))]))))

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

(defn get-svg []
  (-> (.getElementById js/document "d3g")
      (.-firstChild)
      (.-innerHTML)))


(defn graph-editor-page []
  ;; TODO: pass in session instead, and rank g earlier
  (let [selected-id (reagent/atom nil)
        editing (reagent/atom nil)
        g (reaction (session/get :graph))
        keydown-handler
        (fn a-keydown-handler [e]
          (handle-keydown e selected-id editing g))]
    (reagent/create-class
     {:display-name "graph-editor"
      :reagent-render
      (fn graph-editor []
        (let [gr (graph/with-ranks @g)]
          [:div
           [toolbar/toolbar g get-svg]
           [title-editor g editing]
           [:div#d3g [d3/graph gr selected-id g editing]]
           [:div [table gr selected-id editing g]]]))
      :component-did-mount
      (fn graph-editor-did-mount [this]
        (.addEventListener js/document "keydown" keydown-handler))
      :component-will-unmount
      (fn graph-editor-will-unmount [this]
        (.removeEventListener js/document "keydown" keydown-handler))})))
