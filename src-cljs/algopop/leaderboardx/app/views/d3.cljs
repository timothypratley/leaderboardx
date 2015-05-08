(ns algopop.leaderboardx.app.views.d3
  (:require [reagent.core :as reagent]
            [cljsjs.d3]))

(defn d3g
  ([g] (d3g g {}))
  ([{:keys [nodes edges]} existing-nodes]
   (let [ks (keys nodes)
         mids (for [[source targets] edges
                    [target] targets]
                [source target])
         ks (concat mids ks)
         k->idx (into {} (map vector ks (range)))
         nodes (for [k ks]
                 (merge (get existing-nodes k {:id k})
                        (nodes k)))]
     (clj->js {:nodes nodes
               :idx k->idx
               :paths (for [[source targets] edges
                            [target] targets]
                        [(k->idx source)
                         (k->idx [source target])
                         (k->idx target)])
               :links (apply concat (for [[source targets] edges
                                          [target] targets]
                                      [{:id [source target]
                                        :source (k->idx source)
                                        :target (k->idx [source target])}
                                       {:id [source target]
                                        :source (k->idx [source target])
                                        :target (k->idx target)}]))}))))

(defn overwrite [a b]
  (set! (.-length a) 0)
  (.apply (.-push a) a b))

(defn reconcile [g mutable-graph]
  (let [existing (into {} (for [node (.-nodes mutable-graph)]
                            [(:id node) node]))
        replacement (d3g g existing)]
    (overwrite (.-nodes mutable-graph) (.-nodes replacement))
    (set! (.-idx mutable-graph) (.-idx replacement))
    (overwrite (.-paths mutable-graph) (.-paths replacement))
    (overwrite (.-links mutable-graph) (.-links replacement))))

;; TODO: is selected index right? need to update when graph changes?
(defonce selected-id (reagent/atom nil))
(defonce mouse-down (reagent/atom nil))

(defn bnode [node idx mutable-graph force-layout]
  (let [selected? (= (:id node) @selected-id)]
    [:g {:transform (str "translate(" (:x node) "," (:y node) ")"
                         (when selected?
                           " scale(1.25,1.25)"))
         :on-double-click (fn [e]
                            (reset! selected-id nil)
                            (aset mutable-graph "nodes" idx "fixed" 0)
                            (.resume force-layout))
         :on-mouse-down (fn [e]
                          (.stopPropagation e)
                          (reset! mouse-down true)
                          (reset! selected-id (aget mutable-graph "nodes" idx "id"))
                          (aset mutable-graph "nodes" idx "fixed" 1))}
     [:circle.node {:r 20
                    :stroke (if selected?
                              "#6699aa"
                              "#9ecae1")}]
     [:text {:text-anchor "middle"
             :y 18}
      (:rank node)]
     [:text {:text-anchor "middle"
             :y 4}
      (:id node)]]))

(defn average [& args]
  (/ (apply + args) (count args)))

(defn ror [o a]
  (/ (* 180 (js/Math.atan2 o a)) Math.PI))

(defn blink [path mutable-graph force-layout nodes]
  (let [idx (second path)]
    [:g {:on-double-click (fn [e]
                            (reset! selected-id nil)
                            (aset mutable-graph "nodes" idx "fixed" 0)
                            (.resume force-layout))
         :on-mouse-down (fn [e]
                          (.stopPropagation e)
                          (reset! mouse-down true)
                          (reset! selected-id (aget mutable-graph "nodes" idx "id"))
                          (aset mutable-graph "nodes" idx "fixed" 1))}
     [:path.link {:d (apply str (interleave
                                 ["M" "," "Q" "," " " ","]
                                 (for [idx path
                                       dim [:x :y]]
                                   (get-in nodes [idx dim]))))}]
     (let [{x1 :x y1 :y} (get-in nodes [(first path)])
           {:keys [x y id]} (get-in nodes [(second path)])
           {x3 :x y3 :y} (get-in nodes [(nth path 2)])
           mx (average (average x1 x) (average x x3))
           my (average (average y1 y) (average y y3))]
       [:g
        [:polygon {:points "-5,-5 -5,5 7,0"
                   :fill "#9ecae1"
                   :transform (str "translate(" mx "," my
                                   ") rotate(" (ror (- y3 y1) (- x3 x1)) ")"
                                   (when (= id @selected-id)
                                     " scale(1.25,1.25)"))}]])]))

(defn draw-graph [drawable mutable-graph force-layout]
  (let [{:keys [nodes paths]} @drawable]
    (into
     [:svg {:on-mouse-down (fn [e]
                             (reset! mouse-down true)
                             (reset! selected-id nil))
            :on-mouse-up (fn [e]
                           (reset! mouse-down nil))
            :on-mouse-move (fn [e]
                             (when (and @selected-id @mouse-down)
                               (when-let [idx (aget mutable-graph "idx" @selected-id)]
                                 (when-let [node (aget (.-nodes mutable-graph) idx)]
                                   (let [x (- (.-pageX e) 250)
                                         y (- (.-pageY e) 220)]
                                     (set! (.-px node) x)
                                     (set! (.-py node) y))
                                   (.resume force-layout)))))
            :view-box      "0 0 1000 500"}
      [:rect {:width  1000
              :height 500
              :fill   :none
              :stroke :black}]]
     (concat
      (for [path paths]
        [blink path mutable-graph force-layout nodes])
      (for [[node idx] (map vector nodes (range))
            :when (not (vector? (:id node)))]
        [bnode node idx mutable-graph force-layout])))))

(defn create-force-layout [g tick]
  (-> (js/d3.layout.force)
      #_(js/cola.d3adaptor)
      (.nodes (.-nodes g))
      (.links (.-links g))
      (.linkDistance 50)
      (.charge -200)
      ;; TODO: handle resizing
      (.size #js [1000, 500])
      (.on "tick" tick)))

(let [mutable-graph (d3g nil)
      drawable (reagent/atom {})
      force-layout (create-force-layout
                    mutable-graph
                    (fn tick []
                      (reset! drawable (js->clj mutable-graph :keywordize-keys true))))]

  (defn graph [g]
    (reconcile g mutable-graph)
    (.start force-layout)
    [draw-graph drawable mutable-graph force-layout]))
