(ns algopop.leaderboardx.app.views.graph-table
  (:require
    [algopop.leaderboardx.app.db :as db]
    [algopop.leaderboardx.app.db-firebase :as db-firebase]
    [algopop.leaderboardx.app.graph :as graph]
    [algopop.leaderboardx.app.views.common :as common]
    [goog.string :as gstring]
    [clojure.string :as string]
    [reagent.core :as reagent]
    [reagent.ratom :as ratom :refer-macros [reaction]]))

(def delimiter #"[,;]")

(defn split [s]
  (filter seq (map string/trim (string/split s delimiter))))

(defn replace-edges [id selected-id source outs ins]
  (when-let [node-name (first (string/split source delimiter))]
    (let [source (string/trim node-name)]
      (when (seq source)
        ;; TODO: use the edge type, not "likes"
        ;; TODO: only show video and discuss on about and discuss tabs
        (db-firebase/replace-edges id source (split outs) (split ins) "likes")
        (reset! selected-id source)))))

(defn list-edges [edges]
  (string/join ", " (sort @edges)))

(defn add-node [id]
  [:form.form-inline
   {:on-submit
    (fn submit-add [e]
      (.preventDefault e)
      (let [{:keys [name outs ins]} (common/form-data e)]
        (replace-edges id (atom nil) name outs ins)))}
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

(def conjs
  (fnil conj #{}))

(defn collect-by [xs k1 k2]
  (reduce
    (fn [acc x]
      (update acc (get x k1) conjs (get x k2)))
    {}
    xs))

(defn table [id nodes edges selected-id editing node-types edge-types selected-node-type selected-edge-type]
  (let [search-term (reagent/atom "")
        nodes-by-rank (reaction
                        (sort-by :rank @nodes))
        outs (reaction (collect-by (filter #(= @selected-edge-type (:edge/type %)) @edges) :edge/from :edge/to))
        ins (reaction (collect-by (filter #(= @selected-edge-type (:edge/type %)) @edges) :edge/to :edge/from))]
    (fn a-table [id nodes edges selected-id editing node-types edge-types selected-node-type selected-edge-type]
      [:div
       [common/editable-string "search" editing
        (fn [v]
          (reset! search-term v))]
       [add-node id]
       [:table.table.table-responsive
        [:thead
         [:tr
          [:th "Rank"]
          [select-type @node-types editing]
          [select-type @edge-types editing]
          [:th "From"]]]
        (into
         [:tbody]
         (for [{:keys [db/id rank node/name]} @nodes-by-rank
               :let [selected? (= id @selected-id)
                     match? (and (seq @search-term)
                                 (gstring/startsWith name @search-term))
                     outs-string (string/join "," (@outs id))
                     ins-string (string/join "," (@ins id))]]
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
                    (db/name-node id v))]]
            [:td [common/editable-string outs-string editing
                  (fn update-out-edges [m p v]
                    (replace-edges id selected-id name v ins-string))]]
            [:td [common/editable-string ins-string editing
                  (fn update-in-edges [m p v]
                    (replace-edges id selected-id name outs-string v))]]]))]])))

(defn table-view [nodes edges]
  (let [selected-id (reagent/atom nil)
        editing (reagent/atom nil)]
    [table nodes edges selected-id editing]))
