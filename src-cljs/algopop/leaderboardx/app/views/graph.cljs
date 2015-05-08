(ns algopop.leaderboardx.app.views.graph
  (:require [algopop.leaderboardx.app.views.d3 :as d3]
            [algopop.leaderboardx.app.pagerank :as pagerank]
            [goog.dom.forms :as forms]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(defn rand-char []
  (char (+ 97 (rand-int 26))))

(def vowel? #{\a \e \i \o \u})

(defn next-char [c]
  (let [nxt (rand-char)]
    (if (or (vowel? c) (vowel? nxt))
      nxt
      (recur c))))

(defn rand-name []
  (apply str (take (+ 3 (rand-int 3))
                   (iterate next-char (rand-char)))))

(def test-graph
  (let [ks (distinct (repeatedly 10 rand-name))
        nodes (into {} (for [k ks]
                         [k {:hair (rand-nth ["red" "brown" "black" "blonde"])}]))]
    {:nodes nodes
     :edges (into {} (for [k ks]
                       [k (into {} (for [x (remove #{k} (take (+ 1 (rand-int 2)) (shuffle ks)))]
                                     [x {:value 1}]))]))}))

(defonce g (reagent/atom test-graph))


;;;;

(defn with-link [acc [to from]]
  (assoc-in acc [to from] 1))

(defn g2m [g ks]
  (let [n (count (:nodes g))
        k->idx (zipmap ks (range))
        matrix (vec (repeat n (vec (repeat n 0))))]
    (reduce with-link matrix
            (for [[from es] (:edges g)
                  [to v] es]
              [(k->idx to) (k->idx from)]))))

(defn rank [g]
  (let [ks (keys (:nodes g))
        prs (pagerank/pagerank (g2m g ks))]
    (reverse (sort-by second (map vector ks prs)))))

(defn with-rank [[g prev-rank prev-score] [k pagerank]]
  (let [rank (if (= pagerank prev-score)
               prev-rank
               (inc prev-rank))]
    [(-> g
         (assoc-in [:nodes k :rank] rank)
         (assoc-in [:nodes k :pagerank] pagerank))
     rank
     pagerank]))

(defn with-ranks [g]
  (let [ranks (rank g)]
    (println "RANKS" ranks)
    (first (reduce with-rank [g 0 0] ranks))))

;;;;

(defn merge-left [& maps]
  (apply merge (reverse maps)))

;; TODO: move server side but keep an action list to fast UI update
(defn add-em [g source targets]
  (-> g
      (update-in [:nodes] merge-left {source {}} (zipmap targets (repeat {})))
      (update-in [:edges source] merge-left (zipmap targets (repeat {})))))

(defn form-data [form]
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap form)))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn submit [e]
  (.preventDefault e)
  (let [{:keys [source targets]} (form-data (.-target e))]
    (swap! g add-em (string/trim source) (map string/trim (string/split targets #",")))))

(defn handle-resize [e]
  (println "RESIZE" e))

(defn delete-node [g id]
  (-> g
      (update-in [:nodes] dissoc id)
      (assoc :edges
             (into {}
                   (for [[k links] (dissoc (:edges g) id)]
                     [k (dissoc links id)])))))

(defn delete-edge [g [from to]]
  (println from to "EDGE")
  (update-in g [:edges from] dissoc to))

(defn handle-keydown [e]
  (case (.-keyCode e)
    46 (when @d3/selected-id
         (if (string? @d3/selected-id)
           (swap! g delete-node @d3/selected-id)
           (swap! g delete-edge @d3/selected-id)))
    (.log js/console "KEYDOWN" e)))

(defn in-edges [g k]
  (distinct
   (for [[from es] (:edges g)
         [to v] es
         :when (= k to)]
     from)))

(defn graph-page* []
  (let [gr (with-ranks @g)]
  [:div
   [d3/graph gr]
   [:div.row
    [:form.col-md-8 {:on-submit submit}
     [:input {:type "text"
              :name "source"}]
     [:input {:type "text"
              :name "targets"}]
     [:input {:type :submit
              :value "↩"}]
     [:table.table.table-responsive
      [:thead
       [:th "Rank"]
       [:th "Person"]
       [:th "Commended by"]
       [:th "Commends"]]
      (into
       [:tbody]
       (for [[k v] (sort-by (comp :rank val) (:nodes gr))]
         [:tr {:class (when (= k @d3/selected-id)
                        "info")
               :on-click (fn [e]
                           ;; TODO: set selected as well
                           (reset! d3/selected-id k)
                           (println "SELECTED" k))}
          [:td (:rank v)]
          [:td k]
          [:td (string/join ", " (in-edges gr k))]
          [:td (string/join ", " (keys (get-in gr [:edges k])))]]))]]
    [:div.col-md-4
     [:ul.list-unstyled
      [:li "Enter a node name and press ENTER to add it."]
      [:li "Enter a comma separated list of nodes to link to and press ENTER to add them."]
      [:li "Select a node or edge by mouse clicking it and press DEL to delete it."]
      [:li "Drag nodes or edges around by click hold and move."]
      [:li "Double click to unpin nodes and edges."]]]]]))

(defn graph-page []
  ;; TODO: pass in session instead, and rank g earlier
  (reagent/create-class
   {:display-name "graph-page"
    :reagent-render graph-page*
    :component-did-mount
    (fn did-mount [this]
      (.addEventListener js/document "keydown" handle-keydown)
      (.addEventListener js/window "resize" handle-resize))
    :component-will-unmount
    (fn will-unmount [this]
      (.removeEventListener js/document "resize" handle-resize)
      (.removeEventListener js/window "keydown" handle-keydown))}))
