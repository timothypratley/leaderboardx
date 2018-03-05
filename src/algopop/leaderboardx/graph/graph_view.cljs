(ns algopop.leaderboardx.graph.graph-view
  (:require
    [algopop.leaderboardx.app.views.common :as common]
    [algopop.leaderboardx.graph.d3-force-layout :as force]
    [algopop.leaderboardx.graph.graph :as graph]
    [cljs.test]
    [clojure.string :as string]
    [devcards.core :refer [defcard-rg]]
    [reagent.core :as reagent]
    [reagent.dom :as dom]
    [reagent.ratom :as ratom]
    [reanimated.core :as anim]
    [goog.crypt :as crypt])
  (:import
    [goog.crypt Md5]))

(defn color-for [uid]
  (if uid
    (let [h (hash uid)]
      [(bit-and 0xff h)
       (bit-and 0xff (bit-shift-right h 8))
       (bit-and 0xff (bit-shift-right h 16))])
    [255 255 255]))

(defn scale-rgb [rgb rank-scale]
  (map int (map * rgb (repeat (+ 0.9 (* 0.5 rank-scale))))))

(defn scale-dist [n rank-scale]
  (+ 5 (* (min (max n 10) 30) rank-scale)))

(defn rgb [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(defn md5-hash [s]
  (let [md5 (Md5.)]
    (.update md5 s)
    (crypt/byteArrayToHex (.digest md5))))

(defn gravatar-background [id [width height] email]
  (let [guid (md5-hash (string/trim email))
        r (max width height)]
    [:g
     [:defs
      [:pattern
       {:id guid
        :pattern-units "userSpaceOnUse"
        :height (* r 2)
        :width (* r 2)
        :pattern-transform (str "translate(" (- r) "," (- r) ")")}
       [:image
        {:height (* r 2)
         :width (* r 2)
         :xlink-href (str "http://www.gravatar.com/avatar/" guid)}]]]
     [:circle
      {:r r
       :fill (str "url(#" guid ")")}]]))

(defn stringify-points [points]
  (->> points
       (partition-all 2)
       (map #(string/join "," %))
       (string/join " ")))

(defn polygon-background [attrs points]
  [:polygon
   (merge attrs {:points (stringify-points points)})])

(defn triangle-background [attrs [width height]]
  (let [y1 (/ height 2)
        y2 (- height)
        x1 (* 2 width)
        x2 (- x1)
        points [x1 y1 0 y2 x2 y1]]
    [polygon-background attrs points]))

(defn triangle-down-background [attrs [width height]]
  (let [y1 height
        y2 (- (/ height 2))
        x1 (* 2 width)
        x2 (- x1)
        points [x1 y2 0 y1 x2 y2]]
    [polygon-background attrs points]))

(defn rectangle-background [attrs [width height]]
  [:rect
   (merge attrs
          {:x (- width)
           :y (- height)
           :width (* width 2)
           :height (* height 2)})])

(defn square-background [attrs [width height]]
  [:rect
   (let [r height]
     (merge attrs
            {:x (- r)
             :y (- r)
             :width (* r 2)
             :height (* r 2)}))])

(defn ellipse-background [attrs [width height]]
  [:ellipse
   (merge attrs {:rx width
                 :ry height})])

(defn circle-background [attrs [width height]]
  [:circle
   (merge attrs {:r height})])

(def shape-dispatch
  {"circle" circle-background
   "ellipse" ellipse-background
   "triangle" triangle-background
   "triangle-down" triangle-down-background
   "square" square-background
   "rectangle" rectangle-background})

(defn shape-background [shape dimensions node-color rank-scale selected?]
  [(shape-dispatch shape circle-background)
   {:fill node-color
    :stroke (if selected?
              "#6699aa"
              "#9ecae1")
    :style {:cursor "pointer"}}
   dimensions])

(defn email? [s]
  (and (string? s)
       (->> s
            (string/trim)
            (string/upper-case)
            (re-matches #"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}"))))

(defn lines [s width]
  (reduce
    (fn add-word [lines word]
      (if (> (count (last lines)) width)
        (conj lines word)
        (update lines (dec (count lines)) str " " word)))
    [""]
    (-> (string/split s #"\s+")
        (->> (map string/trim)))))

(defn draw-node
  [node-types
   [node-id node]
   node-count
   max-pagerank
   simulation
   mouse-down?
   selected-id
   zoom-factor
   {:keys [shift-click-node double-click-node]}]
  (when-let [idxs (.-idxs simulation)]
    (let [particle (aget (.nodes simulation) (idxs node-id))
          x (.-x particle)
          y (.-y particle)
          defaults (get @node-types (:node/type node "person"))
          {:keys [node/size node/color node/tags node/text node/pagerank node/shape node/name-size uid]} (merge defaults node)
          selected? (= node-id @selected-id)
          rank-scale (if max-pagerank (/ pagerank max-pagerank 0.5) 1)
          count-factor (* (js/Math.sqrt node-count) 5)
          name-font-size (* (or name-size 1) count-factor)
          height (* (or size 1) count-factor rank-scale)
          width (* height (count node-id) 0.3 rank-scale)]
      ^{:key node-id}
      [:g
       {:transform (str "translate(" x "," y ")"
                        (when selected?
                          " scale(1.25,1.25)"))
        :tab-index "1"
        :on-double-click
        (fn node-double-clicked [e]
          (reset! selected-id nil)
          (js-delete particle "fx")
          (js-delete particle "fy")
          (force/restart-simulation simulation)
          (when double-click-node
            (double-click-node node-id)))
        :on-mouse-down
        (fn node-mouse-down [e]
          (.stopPropagation e)
          (.preventDefault e)
          (reset! mouse-down? true)
          (common/blur-active-input)
          (if (and shift-click-node (.-shiftKey e) @selected-id node-id)
            (shift-click-node @selected-id node-id)
            (when (= @selected-id node-id)
              (swap! zoom-factor #(if (= 1.5 %) 3 1.5))))
          (when (not= @selected-id node-id)
            (reset! selected-id node-id)))}
       (if (email? node-id)
         [gravatar-background node-id [height height] node-id]
         [shape-background shape [width height] (or color "white") rank-scale selected?])
       ;; TODO: why can't you click on text to select the node???
       [:text.unselectable
        {:text-anchor "middle"
         :font-size name-font-size
         :style {:pointer-events "none"
                 :dominant-baseline "central"}}
        node-id]
       ;; TODO: refactor all the riddiculous min/max node-count to use the count-factor above
       (when (seq text)
         (into
           [:g]
           (map-indexed
             (fn [idx line]
               [:text.unselectable
                {:y (* (+ 2 idx) (/ (min (max node-count 8) 22) 2))
                 :text-anchor "middle"
                 :font-size (/ (min (max node-count 8) 22) 2)
                 :style {:pointer-events "none"
                         :dominant-baseline "central"}}
                line])
             (lines text 15))))
       (when (seq tags)
         (into
           [:g]
           (map-indexed
             (fn [idx tag]
               [:g
                {:transform (str "translate(0," (/ (* (- (+ 2 idx)) (min (max node-count 8) 22)) 2) ")")}
                [:rect
                 {:fill "#ffeead"
                  :stroke "black"
                  :stroke-width 0.1
                  :rx 2
                  :ry 2
                  :x (- (/ (* 2 (+ 3 (count tag))) 2))
                  :y (- (/ (/ (min (max node-count 8) 22) 2) 2))
                  :width (* 2 (+ 3 (count tag)))
                  :height (/ (min (max node-count 8) 22) 2)}]
                [:text.unselectable
                 {:text-anchor "middle"
                  :font-size (/ (min (max node-count 8) 22) 2)
                  :style {:pointer-events "none"
                          :dominant-baseline "central"}}
                 tag]])
             (-> (string/split tags #",")
                 (->> (map string/trim))))))])))

(defn average [& args]
  (/ (apply + args) (count args)))

(defn rise-over-run [o a]
  (/ (* 180 (js/Math.atan2 o a)) js/Math.PI))

(defn darken [color]
  ;; TODO: implement
  color)

(defn upright [angle]
  (cond
    (< 90 angle) (- angle 180)
    (< angle -90) (+ angle 180)
    :else angle))

(defn draw-edge
  [edge-types
   [[from to :as edge-id] edge]
   simulation
   mouse-down?
   selected-id
   {:keys [shift-click-edge]}]
  (when-let [idxs (.-idxs simulation)]
    (let [idx (idxs edge-id)
          from-idx (idxs from)
          to-idx (idxs to)]
      ;; TODO: isolate data specific stuff here
      (when (and idx from-idx to-idx)
        (let [defaults (get @edge-types (:edge/type edge "likes"))
              {:keys [edge/label edge/weight edge/color edge/dasharray edge/negate]} (merge defaults edge)
              particle (aget (.nodes simulation) idx)
              x2 (.-x particle)
              from-particle (aget (.nodes simulation) from-idx)
              y2 (.-y particle)
              x1 (.-x from-particle)
              y1 (.-y from-particle)
              to-particle (aget (.nodes simulation) to-idx)
              x3 (.-x to-particle)
              y3 (.-y to-particle)
              phi (+ (js/Math.atan2 (- y3 y1) (- x3 x1)) (/ js/Math.PI 2))
              xo (* 2 (js/Math.cos phi))
              yo (* 2 (js/Math.sin phi))
              xo2 (* xo 3)
              yo2 (* yo 3)
              midx (/ (+ x1 x2 x2 x3) 4)
              midy (/ (+ y1 y2 y2 y3) 4)
              selected? (= edge-id @selected-id)]
          ^{:key edge-id}
          [:g
           {:on-double-click
            (fn link-double-click [e]
              (reset! selected-id nil)
              (let [particle (aget (.nodes simulation) idx)]
                (js-delete particle "fx")
                (js-delete particle "fy"))
              (force/restart-simulation simulation))
            :on-mouse-down
            (fn link-mouse-down [e]
              (.stopPropagation e)
              (.preventDefault e)
              (reset! mouse-down? true)
              (reset! selected-id edge-id)
              (common/blur-active-input)
              (when (and shift-click-edge (.-shiftKey e))
                (shift-click-edge edge-id edge)))
            ;; TODO: negate color should be paramatarizable
            :stroke (cond-> (or (when negate "#ff0000") color "#9ecae1")
                            selected? (darken))}
           ;; TODO: what about weights greater than 3?
           (when (not= 2 weight)
             [:path
              {:fill "none"
               :stroke-dasharray dasharray
               :d (str "M " x1 "," y1 " Q " x2 "," y2 " " x3 "," y3)}])
           (when (#{3 2} weight)
             [:path
              {:fill "none"
               :stroke-dasharray dasharray
               :d (str "M " (+ x1 xo) "," (+ y1 yo) " Q " (+ x2 xo) "," (+ y2 yo) " " (+ x3 xo) "," (+ y3 yo))}])
           (when (#{3 2} weight)
             [:path
              {:fill "none"
               :stroke-dasharray dasharray
               :d (str "M " (- x1 xo) "," (- y1 yo) " Q " (- x2 xo) "," (- y2 yo) " " (- x3 xo) "," (- y3 yo))}])
           (when negate
             [:path
              {:fill "none"
               :d (str "M " (- midx (* 2 xo2)) "," (- midy (* 2 yo2)) " L " (+ midx (* 2 xo2)) "," (+ midy (* 2 yo2)))}])
           [:g
            {:transform (str "translate(" midx "," midy ")")}
            [:polygon
             {:points "0,-5 0,5 12,0"
              :fill (cond-> (or color "#9ecae1")
                      selected? (darken))
              :transform (str "rotate(" (rise-over-run (- y3 y1) (- x3 x1)) ")"
                              (when selected?
                                " scale(1.25,1.25)"))
              :style {:cursor "pointer"}}]
            [:text
             {:fill "black"
              :stroke "none"
              :text-anchor "middle"
              :transform (str "rotate("
                              (upright (rise-over-run (- y3 y1) (- x3 x1)))
                              ")"
                              (when selected?
                                " scale(1.25,1.25)"))}
             (string/join ": "
               (remove nil?
                       [(when (not= 1 weight) weight)
                        (when-not (string/blank? label) label)]))]]])))))

(defn bounds [[minx miny maxx maxy] simulation-node]
  [(min minx (.-x simulation-node))
   (min miny (.-y simulation-node))
   (max maxx (.-x simulation-node))
   (max maxy (.-y simulation-node))])

(defn normalize-bounds [[minx miny maxx maxy]]
  (let [width (+ 100 (- maxx minx))
        height (+ 100 (- maxy miny))
        width (max width height)
        height (max height width)
        midx (average maxx minx)
        midy (average maxy miny)]
    [(- midx (/ width 2)) (- midy (/ height 2)) width height]))

(defn initial-bounds [simulation-node]
  (if simulation-node
    [(.-x simulation-node)
     (.-y simulation-node)
     (.-x simulation-node)
     (.-y simulation-node)]
    [0 0 0 0]))

(defn update-bounds [snapshot simulation-nodes]
  reduced
  (assoc snapshot :bounds (normalize-bounds (reduce bounds (initial-bounds (first simulation-nodes)) simulation-nodes))))

(defn draw-svg [show-pageranks? node-types edge-types nodes edges snapshot simulation mouse-down? zooming zoom selected-id zoom-factor callbacks]
  (let [{:keys [bounds]} @snapshot
        max-pagerank (when @show-pageranks?
                       (reduce max (map :node/pagerank (vals @nodes))))
        node-count (count @nodes)]
    [:svg.unselectable
     ;; TODO: reanimated interpolate to
     {:view-box (string/join " " (or zoom bounds))
      :style {:width "100%"
              :height "100%"}}
     ;; These are forced with parens instead of vector because the simulation updated
     (doall
       (concat
         (for [edge @edges]
           (draw-edge edge-types edge simulation mouse-down? selected-id callbacks))
         (for [node @nodes]
           (draw-node node-types node node-count max-pagerank simulation mouse-down? selected-id zoom-factor callbacks))))
     (when-let [[x y width height] zooming]
       [:rect
        {:stroke "black"
         :fill "none"
         :x x
         :y y
         :width width
         :height height}])]))

(defn draw-graph [this show-pageranks? node-types edge-types nodes edges snapshot simulation mouse-down? selected-id zoom-factor callbacks]
  (let [xx (reagent/atom nil)
        yy (reagent/atom nil)
        click-xx (reagent/atom nil)
        click-yy (reagent/atom nil)
        selection-box (reagent/atom nil)
        selecting (reagent/atom nil)
        ;; TODO: oh dear, such reaction
        zoom-x (reagent/atom 0)
        zoom-y (reagent/atom 0)
        zoom-x-spring (anim/spring zoom-x {:damping 10.0})
        zoom-y-spring (anim/spring zoom-y {:damping 10.0})
        zoom-factor-spring (anim/spring zoom-factor {:damping 10.0})]
    (fn a-draw-graph [this show-pageranks? node-types edge-types nodes edges snapshot simulation mouse-down? selected-id zoom-factor callbacks]
      (let [[minx miny width height] (:bounds @snapshot)
            midx (+ minx (/ width 2))
            midy (+ miny (/ height 2))
            selected-zoom (when (and (< 1 @zoom-factor-spring) @zoom-x @zoom-y)
                            [(- @zoom-x-spring (/ width 2 @zoom-factor-spring))
                             (- @zoom-y-spring (/ height 2 @zoom-factor-spring))
                             (/ width @zoom-factor-spring)
                             (/ height @zoom-factor-spring)])]
        (or
          (when @selected-id
            (when-let [idxs (.-idxs simulation)]
              ;; TODO: this pattern is repeated in draw-edge and draw-node, DRY
              (let [particle (aget (.nodes simulation) (idxs @selected-id))]
                (reset! zoom-x (.-x particle))
                (reset! zoom-y (.-y particle)))))
          (do (reset! zoom-x midx)
              (reset! zoom-y midy)
              (reset! zoom-factor 1)))
        [:div
         {:style {:height "60vh"}
          :on-mouse-down
          (fn graph-mouse-down [e]
            (reset! click-xx @xx)
            (reset! click-yy @yy)
            (.preventDefault e)
            (reset! mouse-down? true)
            (reset! selected-id nil)
            (common/blur-active-input))
          :on-mouse-up
          (fn graph-mouse-up [e]
            (let [width (js/Math.abs (- @click-xx @xx))
                  height (js/Math.abs (- @click-yy @yy))]
              (if (and (< 100 width) (< 100 height))
                (do
                  (reset! selection-box [(min @click-xx @xx) (min @click-yy @yy)
                                         width height])
                  (reset! selecting false)
                  (reset! click-xx nil)
                  (reset! click-yy nil))
                (do (reset! selecting false)
                    (reset! click-xx nil)
                    (reset! click-yy nil))))
            (reset! mouse-down? nil))
          :on-mouse-leave
          (fn graph-mouse-leave [e]
            (reset! mouse-down? false)
            (reset! click-xx nil)
            (reset! click-yy nil)
            (reset! selecting false))
          :on-mouse-move
          (fn graph-mouse-move [e]
            ;; TODO: draging while so
            (let [elem (dom/dom-node this)
                  r (.getBoundingClientRect elem)
                  left (.-left r)
                  top (.-top r)
                  width (.-width r)
                  height (.-height r)
                  [bx by bw bh] (or selected-zoom (:bounds @snapshot))
                  cx (+ bx (/ bw 2))
                  cy (+ by (/ bh 2))
                  scale (/ bw (min width height))
                  ex (.-clientX e)
                  ey (.-clientY e)
                  divx (- ex left (/ width 2))
                  divy (- ey top (/ height 2))
                  x (+ (* divx scale) cx)
                  y (+ (* divy scale) cy)]
              (reset! xx x)
              (reset! yy y)
              (when (and @mouse-down? (not @selected-id))
                (let [width (js/Math.abs (- @click-xx @xx))
                      height (js/Math.abs (- @click-yy @yy))]
                  (reset! selecting
                          [(min @click-xx @xx) (min @click-yy @yy)
                           width height])))
              (when (and @selected-id @mouse-down?)
                (let [k @selected-id]
                  (when-let [idx (get (.-idxs simulation) k)]
                    (when-let [particle (aget (.nodes simulation) idx)]
                      (set! (.-fx particle) x)
                      (set! (.-fy particle) y)
                      (force/restart-simulation simulation)))))))}
         ;; todo: don't deref here
         [draw-svg show-pageranks? node-types edge-types nodes edges snapshot simulation mouse-down? @selecting selected-zoom selected-id zoom-factor callbacks]]))))

(defn graph-view [g node-types edge-types selected-id selected-edge-type zoom-factor callbacks]
  (reagent/with-let
    [nodes (ratom/reaction (graph/nodes @g))
     matching-edges (ratom/reaction (graph/edges @g))
     snapshot (reagent/atom {:bounds [0 0 0 0]
                             :particles @nodes})
     simulation (force/create-simulation)
     mouse-down? (reagent/atom nil)
     show-pageranks? (ratom/reaction (:show-pageranks? @g))
     watch (reagent/track!
             (fn a-graph-watcher []
               (force/update-simulation simulation @nodes @matching-edges node-types edge-types)
               (force/restart-simulation simulation)))
     ;; test this, should be inside right?
     _ (.on simulation "tick"
            (fn simulation-tick []
              (swap! snapshot update-bounds (.nodes simulation))))]
    [draw-graph (reagent/current-component) show-pageranks? node-types edge-types nodes matching-edges snapshot simulation mouse-down? selected-id zoom-factor callbacks]
    (finally
      (reagent/dispose! watch)
      (.stop simulation))))

(defcard-rg graph-example
  (fn []
    (let [g (reagent/atom {:nodes [{:index 0 :db/id 0 :name "foo" :uid "zz"}
                                   {:index 1 :db/id 1 :name "bar" :uid "zz"}]
                           :edges {:from 0 :to 1}})
          node-types (reagent/atom {})
          edge-types (reagent/atom {})
          selected-id (reagent/atom nil)
          callbacks {}]
      (fn []
        [:div
         {:style {:border "1px solid black"}}
         [graph-view g node-types edge-types selected-id callbacks]]))))
