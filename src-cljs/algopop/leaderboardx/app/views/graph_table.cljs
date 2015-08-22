(ns algopop.leaderboardx.app.views.graph-table
  (:require
   [algopop.leaderboardx.app.db :as db]
   [algopop.leaderboardx.app.graph :as graph]
   [algopop.leaderboardx.app.views.common :as common]
   [goog.string :as gstring]
   [clojure.string :as string]
   [reagent.core :as reagent]))

;; TODO: convert to editable-string and selectable

(defn submit-add-node-and-edges [e selected-id g]
  (let [{:keys [source outs ins]} (common/form-data e)
        source (string/trim source)
        outs (map string/trim (string/split outs #"[,;]"))
        ins (map string/trim (string/split ins #"[,;]"))]
    (db/replace-edges source outs ins)
    (reset! selected-id source)))

(defn humanize-edges [edges]
  (string/join ", " (sort edges)))

(defn input-row [gr search-term selected-id editing]
  (let [outs (reagent/atom "")
        ins (reagent/atom "")]
    (fn an-input-row [gr search-term selected-id]
      (remove-watch selected-id :selection)
      (add-watch selected-id :selection
                 (fn selection-watcher [k r a b]
                   ;; TODO: set to names
                   ;(reset! search-term b)
                   ;(reset! outs (humanize-edges (keys (get-in gr [:edges b]))))
                   ;(reset! ins (humanize-edges (keys (graph/in-edges gr b))))
                   ))
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

(defn rename-node [id name selected-id editing g]
  [:form
   {:on-submit
    (fn rename-node-submit [e]
      (let [{:keys [text]} (common/form-data e)]
        (swap! g graph/rename-node id text)
        (reset! selected-id text))
      (reset! editing nil))}
   [common/focus-append-input
    {:default-value name}]])

(defn edit-edges [id outs ins selected-id editing g]
  [:form
   {:on-submit
    (fn edit-edges-submit [e]
      (submit-add-node-and-edges e selected-id g)
      (reset! editing nil))}
   [:input {:type "hidden"
            :name "source"
            :value id}]
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
  (let [search-term (reagent/atom "")
        ;; TODO don't
        f #(get-in gr [:nodes % :node/name])]
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
         (for [[id {:keys [rank node/name]}] (sort-by (comp :rank val) (:nodes gr))
               :let [selected? (= id @selected-id)
                     match? (and (seq @search-term) (gstring/startsWith name @search-term))
                     outs (humanize-edges (map f (keys (get-in gr [:edges id]))))
                     ins (humanize-edges (map f (keys (graph/in-edges gr id))))]]
           [:tr
            {:class (cond selected? "info"
                          match? "warning")
             :style {:cursor "pointer"}
             :on-click
             (fn table-row-click [e]
               (reset! selected-id id))}
            [:td rank]
            [:td.editable
             {:style {:cursor "pointer"}
              :on-click
              (fn node-name-click [e]
                (reset! editing :node))}
             (if (and selected? (#{:node} @editing))
               [rename-node id name selected-id editing g]
               [:span
                name
                [:span.glyphicon.glyphicon-pencil.edit]])]
            [:td.editable
             {:style {:cursor "pointer"}
              :on-click
              (fn outs-click [e]
                (reset! editing :outs))}
             (if (and selected? (#{:outs} @editing))
               [edit-edges id outs ins selected-id editing g]
               [:span
                outs
                [:span.glyphicon.glyphicon-pencil.pull-right.edit]])]
            [:td.editable
             {:style {:cursor "pointer"}
              :on-click
              (fn ins-click [e]
                (reset! editing :ins))}
             (if (and selected? (#{:ins} @editing))
               [edit-edges id outs ins selected-id editing]
               [:span
                ins
                [:span.glyphicon.glyphicon-pencil.pull-right.edit]])]]))]))))
