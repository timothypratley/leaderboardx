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

;; TODO: only show video and discuss on about and discuss tabs

(def delimiter #"[,;]")

(defn split [s]
  (filter seq (map string/trim (string/split s delimiter))))

(defn replace-edges [graph-name selected-id source node-type edge-type outs ins]
  (when-let [node-name (first (string/split source delimiter))]
    (let [source (string/trim node-name)]
      (when (seq source)
        (db-firebase/replace-edges graph-name source node-type edge-type (split outs) (split ins))
        (reset! selected-id source)))))

(defn add-node [graph-name selected-node-type selected-edge-type]
  [:form.form-inline
   {:on-submit
    (fn submit-add [e]
      (.preventDefault e)
      (let [{:keys [name outs ins]} (common/form-data e)]
        (replace-edges graph-name (atom nil) name @selected-node-type @selected-edge-type outs ins)))}
   [:input.form-control {:name "name"}]
   [:input.form-control {:name "outs"}]
   [:input.form-control {:name "ins"}]
   [:input.form-control
    {:type "submit"
     :value "Add"}]])

;; TODO: use re-com
(defn select-type [types]
  (let [current-type (reagent/atom (ffirst types))]
    (fn a-select-type [types]
      [:th
       (into
         [:select
          {:on-change
           (fn selection [e]
             (when-let [v (.. e -target -value)]
               (reset! current-type v)
               (common/blur-active-input)))}]
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

(defn table [graph-name nodes edges selected-id node-types edge-types selected-node-type selected-edge-type]
  (let [search-term (reagent/atom "")
        nodes-by-rank (reaction
                        (sort-by :rank @nodes))
        outs (reaction (collect-by (filter #(= @selected-edge-type (:edge/type %)) @edges) :edge/from :edge/to))
        ins (reaction (collect-by (filter #(= @selected-edge-type (:edge/type %)) @edges) :edge/to :edge/from))]
    (fn a-table [graph-name nodes edges selected-id node-types edge-types selected-node-type selected-edge-type]
      [:div
       [common/editable-string "search"
        (fn [v]
          (reset! search-term v))]
       [add-node graph-name selected-node-type selected-edge-type]
       [:table.table.table-responsive
        [:thead
         [:tr
          [:th "Rank"]
          [select-type @node-types]
          [select-type @edge-types]
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
            [:td [common/editable-string name
                  (fn update-node-name [v]
                    ()
                    #_(db/name-node id v))]]
            [:td [common/editable-string outs-string
                  (fn update-out-edges [v]
                    (replace-edges graph-name selected-id name @selected-node-type @selected-edge-type v ins-string))]]
            [:td [common/editable-string ins-string
                  (fn update-in-edges [v]
                    (replace-edges graph-name selected-id name @selected-node-type @selected-edge-type outs-string v))]]]))]])))

(defn table-view [graph-name nodes edges]
  (let [selected-id (reagent/atom nil)]
    [table graph-name nodes edges selected-id]))
