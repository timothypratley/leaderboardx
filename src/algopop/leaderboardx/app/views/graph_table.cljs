(ns algopop.leaderboardx.app.views.graph-table
  (:require
   [algopop.leaderboardx.app.db :as db]
   [algopop.leaderboardx.app.graph :as graph]
   [algopop.leaderboardx.app.views.common :as common]
   [goog.string :as gstring]
   [clojure.string :as string]
   [reagent.core :as reagent]))

(def delimiter #"[,;]")

(defn replace-edges [selected-id source outs ins]
  (when-let [node-name (first (string/split source delimiter))]
    (let [source (string/trim node-name)]
      (when (seq source)
        (let [outs (map string/trim (string/split outs delimiter))
              ins (map string/trim (string/split ins delimiter))]
          ;; TODO: use the edge type, not "likes"
          ;; TODO: only show video and discuss on about and discuss tabs
          (db/replace-edges source outs ins "likes")
          (reset! selected-id source))))))

(defn list-edges [edges]
  (string/join ", " (sort @edges)))

(defn add-node []
  [:form.form-inline
   {:on-submit
    (fn submit-add [e]
      (.preventDefault e)
      (let [{:keys [name outs ins]} (common/form-data e)]
        (replace-edges (atom nil) name outs ins)))}
   [:input.form-control {:name "name"}]
   [:input.form-control {:name "outs"}]
   [:input.form-control {:name "ins"}]
   [:input.form-control
    {:type "submit"
     :value "Add"}]])

;; TODO: use re-com
(defn select-type [types editing]
  (let [current-type (reagent/atom (ffirst types))]
    (fn a-select-type [types editing]
      [:th
       (into
         [:select
          {:on-change
           (fn selection [e]
             (when-let [v (.. e -target -value)]
               (reset! current-type v)
               (reset! editing nil)))}]
         (for [type (keys types)]
           [:option type]))])))

(defn table [selected-id editing node-types edge-types selected-node-type selected-edge-type]
  (let [search-term (reagent/atom "")
        nodes-by-rank (db/nodes-for-table)]
    (fn a-table [selected-id editing node-types edge-types selected-node-type selected-edge-type]
      [:div
       [common/editable-string "search" editing
        (fn [v]
          (reset! search-term v))]
       [add-node]
       [:table.table.table-responsive
        [:thead
         [:tr
          [:th "Rank"]
          [select-type node-types editing]
          [select-type edge-types editing]
          [:th "From"]]]
        (into
         [:tbody]
         (for [{:keys [db/id rank node/name]} @nodes-by-rank
               :let [selected? (= id @selected-id)
                     match? (and (seq @search-term)
                                 (gstring/startsWith name @search-term))
                     outs (list-edges (db/outs id))
                     ins (list-edges (db/ins id))]]
           [:tr
            {:class (cond selected? "info"
                          match? "warning")
             :style {:cursor "pointer"}
             :on-click
             (fn table-row-click [e]
               (reset! selected-id id))}
            [:td rank]
            [:td [common/editable-string name editing
                  (fn update-node-name [m p v]
                    (db/rename-node id v))]]
            [:td [common/editable-string outs editing
                  (fn update-out-edges [m p v]
                    (replace-edges selected-id name v ins))]]
            [:td [common/editable-string ins editing
                  (fn update-in-edges [m p v]
                    (replace-edges selected-id name outs v))]]]))]])))

(defn table-view []
  (let [selected-id (reagent/atom nil)
        editing (reagent/atom nil)]
    [table selected-id editing]))
