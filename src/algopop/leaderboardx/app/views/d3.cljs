(ns algopop.leaderboardx.app.views.d3
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.db :as db]
            [cljsjs.d3]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [reagent.ratom :as ratom :include-macros]
            [goog.crypt])
  (:import [goog.crypt Md5]))

(defn overwrite [k x y]
  (let [a (aget x k)
        b (aget y k)]
    (set! (.-length a) 0)
    (.apply (.-push a) a b)))

(defn assign [k a b]
  (aset a k (aget b k)))

(defn d3-graph [nodes edges]
  (let [d3nodes (concat nodes edges)
        id->idx (zipmap (map :id d3nodes) (range))
        d3nodes (map walk/stringify-keys d3nodes)]
    (clj->js
     {:title "Untitled"
      :nodes (map val d3nodes)
      :idx id->idx
      :paths (for [{:keys [db/id from to]} edges]
               [(id->idx from)
                (id->idx id)
                (id->idx to)])
      :links (apply
              concat
              (for [{:keys [db/id from to]} edges]
                [{:link [from id]
                  :source (id->idx from)
                  :target (id->idx id)}
                 {:link [id to]
                  :source (id->idx id)
                  :target (id->idx to)}]))})))

(defn update-d3graph [d3graph nodes edges]
  (let [replacement (d3-graph nodes edges)]
    (assign "title" d3graph replacement)
    (overwrite "nodes" d3graph replacement)
    (assign "idx" d3graph replacement)
    (overwrite "paths" d3graph replacement)
    (overwrite "links" d3graph replacement)))

(def node-color [213 210 255])

(defn scale-rgb [rgb rank-scale]
  (map int (map * rgb (repeat (+ 0.9 (* 0.5 rank-scale))))))

(defn scale-dist [n rank-scale]
  (+ 5 (* (min (max n 10) 30) rank-scale)))

(defn rgb [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(defn md5-hash [s]
  (let [md5 (goog.crypt.Md5.)]
    (.update md5 s)
    (.byteArrayToHex goog.crypt (.digest md5))))

;; TODO: try rounded edge square?
(defn gravatar-background [id r email]
  (let [guid (md5-hash (string/trim email))]
    ;; TODO: Replace when added to react
    [:g {:dangerouslySetInnerHTML
         {:__html (str "<defs>
   <pattern id=\"" guid "\" patternUnits=\"userSpaceOnUse\" height=\"" (* r 2) "\" width=\"" (* r 2) "\" patternTransform=\"translate(" (- r) "," (- r) ")\">
     <image height=\"" (* r 2) "\" width=\"" (* r 2) "\" xlink:href=\"http://www.gravatar.com/avatar/" guid "\"></image>
   </pattern>
  </defs>
  <circle r=\"" r "\" fill=\"url(#" guid ")\"/>")}}]))

(defn stringify-points [points]
  (->> points
       (partition-all 2)
       (map #(string/join "," %))
       (string/join " ")))

(defn polygon-background [attrs points]
  [:polygon
   (merge attrs {:points (stringify-points points)})])

(defn triangle-background [attrs r]
  (let [h (Math/sqrt (- (* 4 r r) (* r r)))
        y1 (- (/ h 3))
        y2 (- (* 2 y1))
        points [(- r) y1 r y1 0 y2]]
    [polygon-background attrs points]))

(defn rect-background [attrs r]
  [:rect
   (merge attrs
          {:x (- r)
           :y (- r)
           :width (* r 2)
           :height (* r 2)})])

(defn circle-background [attrs r]
  [:circle
   (merge attrs {:r r})])

(def shapes
  {:circle circle-background
   :triangle triangle-background
   :square rect-background})

(defn shape-background [shape r node-color rank-scale selected?]
  [(shapes shape circle-background)
   {:fill (rgb (scale-rgb node-color rank-scale))
    :stroke (if selected?
              "#6699aa"
              "#9ecae1")
    :style {:cursor "pointer"}}
   r])

(defn email? [s]
  (->> s
       (string/trim)
       (string/upper-case)
       (re-matches #"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}")))

(def next-shape
  (zipmap (keys shapes) (rest (cycle (keys shapes)))))

(defn draw-node [{:keys [id name x y rank pagerank shape]} n max-pagerank idx d3graph force-layout mouse-down? selected-id root editing]
  (let [selected? (= id @selected-id)
        rank-scale (if max-pagerank (/ pagerank max-pagerank) 0.5)
        r (scale-dist n rank-scale)]
    [:g
     {:transform (str "translate(" x "," y ")"
                      (when selected?
                        " scale(1.25,1.25)"))
      :on-double-click
      (fn node-double-click [e]
        (reset! selected-id nil)
        (aset d3graph "nodes" idx "fixed" 0)
        (.resume force-layout))
      :on-mouse-down
      (fn node-mouse-down [e]
        (.stopPropagation e)
        (.preventDefault e)
        (let [new-selected-id (aget d3graph "nodes" idx "id")]
          (when (and (.-shiftKey e) @selected-id new-selected-id)
            (if (= @selected-id new-selected-id)
              (swap! root update-in [:nodes new-selected-id :shape] next-shape :triangle)
              (swap! root graph/with-edge [@selected-id new-selected-id])))
          (reset! selected-id new-selected-id)
          (reset! editing nil))
        (reset! mouse-down? true)
        (aset d3graph "nodes" idx "fixed" 1))}
     (if false #_(email? id)
         [gravatar-background id r id]
         [shape-background (keyword shape) r node-color rank-scale selected?])

     [:text.unselectable {:text-anchor "middle"
                          :font-size (min (max n 8) 22)
                          :style {:pointer-events "none"
                                  :dominant-baseline "central"}}
      name]]))

(defn average [& args]
  (/ (apply + args) (count args)))

(defn rise-over-run [o a]
  (/ (* 180 (js/Math.atan2 o a)) Math.PI))

(defn draw-link [[from mid to :as path] nodes d3graph force-layout mouse-down? selected-id root editing]
  (let [{x1 :x y1 :y} (get nodes from)
        {x2 :x y2 :y id :id} (get nodes mid)
        {x3 :x y3 :y} (get nodes to)
        selected? (= id (js->clj @selected-id))]
    [:g
     {:on-double-click
      (fn link-double-click [e]
        (reset! selected-id nil)
        (aset d3graph "nodes" mid "fixed" 0)
        (.resume force-layout))
      :on-mouse-down
      (fn link-mouse-down [e]
        (.stopPropagation e)
        (.preventDefault e)
        (reset! mouse-down? true)
        (reset! selected-id (aget d3graph "nodes" mid "id"))
        (reset! editing nil)
        (when (and (.-shiftKey e) @selected-id)
          (let [[from to] @selected-id]
            (swap! root update-in [:edges from to :weight]
                   #(if % nil 1))))
        (aset d3graph "nodes" mid "fixed" 1))
      :stroke (if selected?
                "#6699aa"
                "#9ecae1")}
     [:path
      {:fill "none"
       :stroke-dasharray (when-let [w (get-in @root [:edges from to :weight])]
                           (str w "," 5))
       :d (apply str (interleave
                      ["M" "," " " "," " " ","]
                      (for [idx path
                            dim [:x :y]]
                        (get-in nodes [idx dim]))))}]
     [:polygon
      {:points "-5,-5 -5,5 7,0"
       :fill "#9ecae1"
       :transform (str "translate(" x2 "," y2
                       ") rotate(" (rise-over-run (- y3 y1) (- x3 x1)) ")"
                       (when selected?
                         " scale(1.25,1.25)"))
       :style {:cursor "pointer"}}]]))

(defn bounds [[minx miny maxx maxy] {:keys [x y]}]
  [(min minx x) (min miny y) (max maxx x) (max maxy y)])

(defn normalize-bounds [[minx miny maxx maxy]]
  (let [width (+ 100 (- maxx minx))
        height (+ 100 (- maxy miny))
        width (max width height)
        height (max height width)
        midx (average maxx minx)
        midy (average maxy miny)]
    [(- midx (/ width 2)) (- midy (/ height 2)) width height]))

(defn update-bounds [g]
  (assoc g :bounds (normalize-bounds (reduce bounds [400 400 600 600] (:nodes g)))))

(defn draw-svg [drawable d3graph force-layout mouse-down? selected-id root editing]
  (let [{:keys [nodes paths title bounds]} @drawable
        max-pagerank (reduce max (map :pagerank nodes))]
    (into
     [:svg.unselectable
      {:view-box (string/join " " bounds)
       :style {:width "100%"
               :height "100%"}}]
     (concat
      (for [path paths]
        [draw-link path nodes d3graph force-layout mouse-down? selected-id root editing])
      (for [[node idx] (map vector (remove :to nodes) (range))]
        [draw-node node (count nodes) max-pagerank idx d3graph force-layout mouse-down? selected-id root editing])))))

(defn draw-graph [this drawable d3graph force-layout mouse-down? selected-id root editing]
  [:div
   {:style {:height "60vh"}
    :on-mouse-down
    (fn graph-mouse-down [e]
      (.preventDefault e)
      (reset! mouse-down? true)
      (reset! selected-id nil)
      (reset! editing nil))
    :on-mouse-up
    (fn graph-mouse-up [e]
      (reset! mouse-down? nil))
    ;; TODO: relative deltas from drag start? what about scroll?
    :on-mouse-move
    (fn graph-mouse-move [e]
      (let [elem (dom/dom-node this)
            r (.getBoundingClientRect elem)
            left (.-left r)
            top (.-top r)
            width (.-width r)
            height (.-height r)
            [bx by bw bh] (:bounds @drawable)
            cx (+ bx (/ bw 2))
            cy (+ by (/ bh 2))
            scale (/ bw (min width height))
            ex (.-clientX e)
            ey (.-clientY e)
            divx (- ex left (/ width 2))
            divy (- ey top (/ height 2))
            x (+ (* divx scale) cx)
            y (+ (* divy scale) cy)]
        (when (and @selected-id @mouse-down?)
          (let [k (if (string? @selected-id)
                    @selected-id
                    (pr-str (js->clj @selected-id)))]
            (when-let [idx (aget d3graph "idx" k)]
              (when-let [node (aget d3graph "nodes" idx)]
                (aset node "px" x)
                (aset node "py" y)
                (.resume force-layout)))))))}
   [draw-svg drawable d3graph force-layout mouse-down? selected-id root editing]])

(defn create-force-layout [d3graph tick]
  (-> (js/d3.forceSimulation (.-nodes d3graph))
      (.force "charge" (js/d3.forceManyBody))
      (.force "link" (js/d3.forceLink (.-links d3graph)))
      (.force "center" (js/d3.forceCenter))
      ;;(.size #js [1000, 1000])
      (.on "tick" tick)))

(defn update-db [d3graph]
  (db/update-nodes
   (for [node (.-nodes d3graph)]
     (cond-> {:db/id (.-id node)
              :x (.-x node)
              :y (.-y node)}
       (.-fixed node) (assoc :pinned? true)))))

(defn graph [selected-id root editing]
  (let [nodes (db/watch-nodes)
        edges (db/watch-edges)
        depg (ratom/reaction
               {:nodes @nodes
                :edges @edges})
        d3graph (d3-graph @edges @nodes)
        drawable (reagent/atom {:bounds [400 400 600 600]})
        force-layout (create-force-layout
                      d3graph
                      (fn layout-tick []
                        (update-db d3graph)
                        (reset! drawable (js->clj d3graph :keywordize-keys true))
                        (swap! drawable update-bounds)))
        mouse-down? (reagent/atom nil)]
    (add-watch depg :watch-graph
               (fn a-graph-watcher [k r a b]
                 (when (not= a b)
                   (update-d3graph d3graph (:nodes b) (:edges b)))))
    (reagent/create-class
     {:display-name "graph"
      :reagent-render
      (fn graph-render [selected-id root editing]
        [draw-graph (reagent/current-component) drawable d3graph force-layout mouse-down? selected-id root editing])})))
