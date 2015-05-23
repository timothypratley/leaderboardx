(ns algopop.leaderboardx.app.views.d3
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
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
;; TODO: move these to per component

(defn draw-node [node idx mutable-graph force-layout]
  (let [selected? (= (:id node) @selected-id)]
    [:g {:transform (str "translate(" (:x node) "," (:y node) ")"
                         (when selected?
                           " scale(1.25,1.25)"))
         :on-double-click (fn node-double-click [e]
                            (reset! selected-id nil)
                            (aset mutable-graph "nodes" idx "fixed" 0)
                            (.resume force-layout))
         :on-mouse-down (fn node-mouse-down [e]
                          (.stopPropagation e)
                          (reset! mouse-down true)
                          (reset! selected-id (aget mutable-graph "nodes" idx "id"))
                          (aset mutable-graph "nodes" idx "fixed" 1))}
     [:circle {:r 20
               :fill "#ddddff"
               :stroke (if selected?
                         "#6699aa"
                         "#9ecae1")
               :stroke-width "1.5px"
               :style {:cursor "pointer"}}]
     [:text.unselectable {:text-anchor "middle"
                          :y 18
                          :style {:pointer-events "none"}}
      (:rank node)]
     [:text.unselectable {:text-anchor "middle"
                          :y 4
                          :style {:pointer-events "none"}}
      (:id node)]]))

(defn average [& args]
  (/ (apply + args) (count args)))

(defn ror [o a]
  (/ (* 180 (js/Math.atan2 o a)) Math.PI))

(defn draw-link [path mutable-graph force-layout nodes]
  (let [idx (second path)]
    [:g {:on-double-click (fn link-double-click [e]
                            (reset! selected-id nil)
                            (aset mutable-graph "nodes" idx "fixed" 0)
                            (.resume force-layout))
         :on-mouse-down (fn link-mouse-down [e]
                          (.stopPropagation e)
                          (reset! mouse-down true)
                          (reset! selected-id (aget mutable-graph "nodes" idx "id"))
                          (aset mutable-graph "nodes" idx "fixed" 1))}
     [:path {:fill "none"
             :stroke "#9ecae1"
             :stroke-width "1.5px"
             :d (apply str (interleave
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

(defn draw-svg [drawable mutable-graph force-layout]
  (let [{:keys [nodes paths]} @drawable]
    (into
     [:svg
      {:view-box (str "0 0 " 1000 " " 500)
       :xmlns "http://www.w3.org/2000/svg"
       :version "1.1"}]
     (concat
      (for [path paths]
        [draw-link path mutable-graph force-layout nodes])
      (for [[node idx] (map vector nodes (range))
            :when (not (vector? (:id node)))]
        [draw-node node idx mutable-graph force-layout])
      [[:rect {:width  1000
               :height 500
               :fill   :none
               :stroke :black}]]))))

;; TODO: doesn't work
(defn save-svg [node]
  (let [link (.createElement js/document "a")]
    (set! (.-download link) "graph.svg")
    (set! (.-href link) (str "data:image/svg+xml;utf8,"
                             (js/encodeURIComponent
                              (str
                               "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">"
                               (string/replace (.-innerHTML node) #" data-reactid=\"[^\"]*\"" "")
                               "</svg>"))))
    (.click link)))

(defn draw-graph [size drawable mutable-graph force-layout]
  [:div {:on-mouse-down (fn graph-mouse-down [e]
                          (reset! mouse-down true)
                          (reset! selected-id nil))
         :on-mouse-up (fn graph-mouse-up [e]
                        (reset! mouse-down nil))
         :on-mouse-move (fn graph-mouse-move [e]
                          (when (and @selected-id @mouse-down)
                            (when-let [idx (aget mutable-graph "idx" @selected-id)]
                              (when-let [node (aget mutable-graph "nodes" idx)]
                                (let [{:keys [width height left top]} @size
                                      divx (- (.-clientX e) left)
                                      divy (- (.-clientY e) top)
                                      x (/ (* 1000 divx) width)
                                      y (/ (* 500 divy) height)]
                                  (set! (.-px node) x)
                                  (set! (.-py node) y)
                                  (.resume force-layout))))))}
   [draw-svg drawable mutable-graph force-layout]
   [:button.btn.btn-default.pull-right
    {:on-click (fn save-svg-click [e]
                 (.log js/console (.-firstChild (.getDOMNode (:component @size))))
                 (save-svg (.-firstChild (.getDOMNode (:component @size)))))}
    "Save Image"]])

(defn create-force-layout [g tick]
  (-> (js/d3.layout.force)
      #_(js/cola.d3adaptor)
      (.nodes (.-nodes g))
      (.links (.-links g))
      (.linkDistance 50)
      (.charge -200)
      (.size #js [1000, 500])
      (.on "tick" tick)))

(defn resize [size]
  (let [elem (.getDOMNode (:component size))
        r (.getBoundingClientRect elem)]
    (assoc size
           :width (.-offsetWidth elem)
           :height (.-offsetHeight elem)
           :left (.-left r)
           :top (.-top r))))

(defn graph [g]
  (let [mutable-graph (d3g nil)
        drawable (reagent/atom {})
        force-layout (create-force-layout
                      mutable-graph
                      (fn layout-tick []
                        (reset! drawable (js->clj mutable-graph
                                                  :keywordize-keys true))))
        size (reagent/atom {})
        resize-handler (fn a-resize-handler [e]
                         (swap! size resize))]
    (reagent/create-class
     {:display-name "graph"
      :reagent-render
      (fn graph-render [g]
        (reconcile g mutable-graph)
        (.start force-layout)
        [draw-graph size drawable mutable-graph force-layout])
      :component-did-mount
      (fn graph-did-mount [this]
        (reset! size (resize {:component this}))
        (.addEventListener js/window "resize" resize-handler))
      :component-will-unmount
      (fn graph-will-unmount [this]
        (.removeEventListener js/window "resize" resize-handler))})))
