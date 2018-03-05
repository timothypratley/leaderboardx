(ns algopop.leaderboardx.graph.shortest-path
  (:require [algopop.leaderboardx.graph.shortest-path-visualize :as visualize]
            [algopop.leaderboardx.graph.graph :as graph]))

(defn min-edge [candidates]
  (let [[to {:keys [from cost]}]
        ;; TODO: use a priority queue
        (first (sort-by (comp :cost val) candidates))]
    [to from cost]))

(defn heuristic [g from to]
  (let [{x1 :x, y1 :y} (get-in @g [:nodes from])
        {x2 :x, y2 :y} (get-in @g [:nodes to])]
    (if (and x1 x2 y1 y2)
      (js/Math.sqrt (+ (js/Math.pow (- x2 x1) 2)
                       (js/Math.pow (- y2 y1) 2)))
      0)))

(defn add-candidates [g cost current-node target-node visited candidates]
  (apply dissoc
         (merge candidates
                (for [[to {:keys [edge/weight] :as v}]
                      ;; TODO: make a proper lookup
                      (get (graph/out-edges @g) current-node)
                      :let [new-cost (+ cost
                                        (or weight 1)
                                        (heuristic g to target-node))]
                      :when (let [existing-cost (:cost (candidates to))]
                              (or (not existing-cost)
                                  (< new-cost existing-cost)))]
                  [to {:from current-node
                       :cost new-cost}]))
         (keys visited)))

(declare expand)

(defn start [g start-node target-node]
  (visualize/visualize-start g start-node target-node)
  #(expand g 0 start-node target-node {start-node nil} {}))

(defn visit [g target-node visited candidates]
  (let [[to from cost :as edge] (min-edge candidates)
        visited-result (conj visited edge)
        candidates-result (dissoc candidates to)]
    (visualize/visualize-visit g to from visited-result candidates-result cost)
    (cond
      (= to target-node) #(visualize/visualize-solution g visited-result target-node cost)
      (empty? candidates) #(visualize/visualize-fail g)
      :else #(expand g cost to target-node visited-result candidates-result))))

(defn expand [g cost current-node target-node visited candidates]
  (let [expanded-candidates (add-candidates g cost current-node target-node visited candidates)]
    (visualize/visualize-expand g visited candidates expanded-candidates)
    #(visit g target-node visited expanded-candidates)))

(defn shortest-path [g start-node target-node step-ms searching?]
  (visualize/slow-trampoline step-ms searching? start g start-node target-node))
