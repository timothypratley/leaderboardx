(ns algopop.leaderboardx.app.views.graph
  (:require [reagent.core :as reagent]
            [clojure.string :as string]
            [cljsjs.d3]))

(defn create-force-layout [g tick]
  (-> (js/cola.d3adaptor)
      (.nodes (.-nodes g))
      (.links (.-links g))
      (.linkDistance 50)
      (.size #js [800, 400])
      (.on "tick" tick)))

(def test-graph-old
  {:nodes {"susan" {:hair "brown"}
           "sam" {:hair "red"}
           "bobby" {:hair "black"}
           "kate" {:hair "blonde"}}
   :edges {"susan" {"sam" {:value 3}
                    "kate" {:value 2}}
           "bobby" {"kate" {:value 5}}
           "sam" {"kate" {:value 4}}
           "kate" {"susan" {:value 1}
                   "sam" {:value 2}}}})

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
  (let [ks (distinct (repeatedly 20 rand-name))
        nodes (into {} (for [k ks]
                         [k {:hair (rand-nth ["red" "brown" "black" "blonde"])}]))]
    {:nodes nodes
     :edges (into {} (for [k ks]
                       [k (into {} (for [x (take 3 (shuffle ks))]
                                     [x {:value 1}]))]))}))

(defn d3g
  ([g] (d3g g {}))
  ([{:keys [nodes edges]} existing-nodes]
   (let [ks (keys nodes)
         k->idx (into {} (map vector ks (range)))]
     (clj->js {:nodes (for [k ks]
                        (get existing-nodes k {:id k}))
               :links (for [[source targets] edges
                            [target] targets]
                        {:source (k->idx source)
                         :target (k->idx target)})}))))

(defn overwrite [a b]
  (set! (.-length b) 0)
  (.apply (.-push a) a b))

(defn reconcile [g mutable-graph]
  (let [existing (into {} (for [node (.-nodes mutable-graph)]
                            [(:id node) node]))
        replacement (d3g g existing)]
    (overwrite (.-nodes mutable-graph) (.-nodes replacement))
    (overwrite (.-links mutable-graph) (.-links replacement))))

;; TODO: is dragging index right? need to update when graph changes?
(def dragging (reagent/atom nil))

(defn allow-drop [e]
  (.preventDefault e))

(defn bnode [node idx attrs]
  [:circle.node
   (merge {:cx (:x node)
           :cy (:y node)
           :r 5
           :on-mouse-down (fn [e]
                            (.stopPropagation e)
                            (swap! dragging #(if % nil idx)))}
          attrs)])

(defn draw-graph [drawable mutable-graph force-layout]
  (let [{:keys [nodes links]} @drawable]
    (into
     [:svg {:style {:width 1024 :height 800
                    :position "absolute"
                    :top 100
                    :left 200}
            :on-mouse-down (fn [e]
                             (reset! dragging nil))
            :on-mouse-move (fn [e]
                             (when @dragging
                               (when-let [node (aget (.nodes force-layout) @dragging)]
                                 (set! (.-fixed node) 1)
                                 (set! (.-px node) (- (.-pageX e) 200))
                                 (set! (.-py node) (- (.-pageY e) 100))
                                 (.resume force-layout))))
            ;;:view-box "0 0 300 200"
            }]
     (concat
      (for [{:keys [source target]} links]
        [:line.link {:x1 (:x source) :y1 (:y source)
                     :x2 (:x target) :y2 (:y target)}])
      (for [[node idx] (map vector nodes (range))]
        [bnode node idx
         (when (= idx @dragging)
           {:r 10})])))))

(let [mutable-graph (d3g test-graph)
      drawable (reagent/atom {})
      force-layout (create-force-layout
                    mutable-graph
                    (fn tick []
                      (reset! drawable (js->clj mutable-graph :keywordize-keys true))))]

  (defn d3-graph [g]
    (reconcile g mutable-graph)
    (.start force-layout)
    [draw-graph drawable mutable-graph force-layout]))

;; TODO: pass in session instead
(defn graph-page []
  [:div
   [:form
    [:dl.dl-horizontal
     [:dt [:input {:type "text" :name "source"}]]
     [:dd [:input {:type "text" :name "targets"}]]]]
   (into [:dl.dl-horizontal]
         (apply concat
                (for [[k v] (:nodes test-graph)]
                  [[:dt k]
                   [:dd (string/join ", " (keys (get-in test-graph [:edges k])))]])))
   [d3-graph test-graph]])
