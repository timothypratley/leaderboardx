(ns algopop.leaderboardx.graph.d3-force-layout
  (:require [cljsjs.d3]))

(defn create-simulation []
  (-> (js/d3.forceSimulation #js [])
      (.force
        "charge"
        (doto (js/d3.forceManyBody)
          (.distanceMax 300)
          (.strength -100)))
      (.force
        "link"
        (doto (js/d3.forceLink #js [])
          (.distance
            (fn [link idx]
              (or (.-distance link) 30)))))))

(defn with-index
  "Because D3 uses arrays and we might delete an item in the middle of the array,
  we need to re-index the arrays when updating."
  [xs]
  (map-indexed
    (fn [idx x]
      (assoc x :index idx))
    xs))

(defn update-simulation
  "A simulation expects the graph as an object of nodes and links.
  We need to convert maps of nodes to arrays of objects with indexes for mapping back to their id.
  Edges are treated as nodes for the simulation so that they have mass and curve."
  [simulation nodes edges node-types edge-types]
  (let [simulation-particles (js->clj (.nodes simulation) :keywordize-keys true)
        kept-particles (for [{:keys [id] :as p} simulation-particles
                             :let [entity (or (nodes id) (edges id))]
                             :when entity]
                         (merge (select-keys p [:id :x :y :vx :vy :fx :fy]) entity))
        existing-ids (set (keys (.-idxs simulation)))
        nodes-and-edges (merge nodes edges)
        added-particles (apply dissoc nodes-and-edges existing-ids)
        final-particles (concat kept-particles
                                (for [[k v] added-particles]
                                  (assoc v :id k)))
        idxs (zipmap (map :id final-particles) (range))]
    (.nodes simulation (clj->js (with-index final-particles)))
    (.links
      (.force simulation "link")
      (clj->js
        (with-index
          (apply
            concat
            (for [[[from to :as edge-id] {:keys [edge/type edge/distance]}] edges
                  :let [idx (idxs edge-id)
                        from-idx (idxs from)
                        to-idx (idxs to)
                        distance (or distance (get-in @edge-types [type :edge/distance]) 30)]
                  :when (and idx from-idx to-idx)]
              (cond->
                ;; TODO: link for what? distance does it work? (other force?)
                [{:link [from edge-id]
                  :source from-idx
                  :target idx
                  :distance distance}
                 {:link [edge-id to]
                  :source idx
                  :target to-idx
                  :distance distance}]
                distance
                (conj {:link [from to]
                       :source from-idx
                       :target to-idx
                       :distance (* 3 distance)})))))))
    (set! (.-name simulation) "Untitled")
    (set! (.-idxs simulation) idxs)))

(defn restart-simulation [simulation]
  (doto simulation
    (.restart)
    (.alpha 1)))
