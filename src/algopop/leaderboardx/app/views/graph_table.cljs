(ns algopop.leaderboardx.app.views.graph-table
  (:require
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

(defn replace-edges [graph selected-id source node-type edge-type outs ins]
  (when-let [source (first (split source))]
    (swap! graph
           graph/replace-edges
           source
           node-type
           edge-type
           (set (split outs))
           (set (split ins)))
    (reset! selected-id source)))

(defn add-node [graph selected-id selected-node-type selected-edge-type search-term]
  [:form.form-inline
   {:on-submit
    (fn submit-add [e]
      (.preventDefault e)
      (let [{:keys [name outs ins]} (common/form-data e)]
        (replace-edges graph selected-id name @selected-node-type @selected-edge-type outs ins)))}
   [:input.form-control
    {:name "name"
     :on-change
     (fn [e]
       (reset! search-term (.. e -target -value))
       (when ((:nodes @graph) @search-term)
         (reset! selected-id @search-term)))}]
   ;; TODO: make reactive or remove
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

;; TODO: remove if don't need
(defn collect-by [xs k1 k2]
  (reduce
    (fn [acc [id x]]
      (update acc (get x k1) conjs (get x k2)))
    {}
    xs))

;; TODO: can transients support update?
(defn collect [xs]
  (reduce (fn [acc [k v]]
            (update acc k conjs v))
          {}
          xs))

(defn table [g selected-id node-types edge-types selected-node-type selected-edge-type]
  (let [search-term (reagent/atom "")
        nodes-by-rank (reaction
                        (sort-by #(vector (:rank (val %)) (key %)) (:nodes @g)))
        matching-edges (reaction
                         (doall
                           (for [[from tos] (:edges @g)
                                 [to {:keys [edge/type]}] tos
                                 :when (= type @selected-edge-type)]
                             [from to])))
        outs (reaction (collect @matching-edges))
        ins (reaction (collect (map reverse @matching-edges)))]
    (fn a-table [graph selected-id node-types edge-types selected-node-type selected-edge-type]
      [:div
       [add-node graph selected-id selected-node-type selected-edge-type search-term]
       [:table.table.table-responsive
        [:thead
         [:tr
          [:th "Rank"]
          [select-type @node-types]
          [select-type @edge-types]
          [:th "From"]]]
        (into
         [:tbody]
         (for [[id {:keys [rank node/name]}] @nodes-by-rank
               :let [selected? (= id @selected-id)
                     match? (and (seq @search-term)
                                 (gstring/startsWith (or name id) @search-term))
                     out-ids (@outs id)
                     in-ids (@ins id)
                     outs-string (string/join ", " out-ids)
                     ins-string (string/join ", " in-ids)]
               :when (or (not @selected-id)
                         (= id @selected-id)
                         (contains? out-ids @selected-id)
                         (contains? in-ids @selected-id))]
           [:tr
            {:class (cond selected? "info"
                          match? "warning")
             :style {:cursor "pointer"}
             :on-click
             (fn table-row-click [e]
               (reset! selected-id id))}
            [:td rank]
            ;; TODO: names vs ids omg
            [:td [common/editable-string (or name id)
                  (fn update-node-name [v]
                    (let [new-name (string/trim v)]
                      (when (and (seq new-name)
                                 (not= new-name (or name id)))
                        (swap! graph graph/rename-node id new-name)
                        (reset! selected-id new-name))))]]
            [:td [common/editable-string outs-string
                  (fn update-out-edges [v]
                    ;; TODO: make edge type selection make sense
                    (replace-edges graph selected-id (or name id) @selected-node-type @selected-edge-type v ins-string)
                    (common/blur-active-input))]]
            [:td [common/editable-string ins-string
                  (fn update-in-edges [v]
                    (replace-edges graph selected-id (or name id) @selected-node-type @selected-edge-type outs-string v)
                    (common/blur-active-input))]]]))]])))

(defn table-view [graph nodes edges]
  (let [selected-id (reagent/atom nil)]
    [table graph nodes edges selected-id]))
