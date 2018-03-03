(ns algopop.leaderboardx.graph.shortest-path
  (:require [algopop.leaderboardx.graph.shortest-path-visualize :as visualize]
            [algopop.leaderboardx.graph.graph :as graph]
            [task.core :as task]))

(defn min-edge [candidates]
  (let [[to {:keys [edge/from distance]}]
        (first (sort-by (comp :distance val) candidates))]
    [to from distance]))

(defn heuristic [g from to]
  (let [{x1 :x, y1 :y} (get-in @g [:nodes from])
        {x2 :x, y2 :y} (get-in @g [:nodes to])]
    (if (and x1 x2 y1 y2)
      (js/Math.sqrt (+ (js/Math.pow (- x2 x1) 2)
                       (js/Math.pow (- y2 y1) 2)))
      0)))

(defn add-candidates [g distance current-node target-node visited candidates]
  (apply dissoc
         (merge candidates
                (for [[to {:keys [edge/weight] :as v}]
                      ;; TODO: make a proper lookup
                      (get (graph/out-edges @g) current-node)
                      :let [d (+ distance
                                 (or weight 1)
                                 (heuristic g to target-node))]
                      :when
                      (or (not (candidates to))
                          (< d (:distance (candidates to))))]
                  [to (assoc
                        (select-keys v [:edge/weight])
                        :edge/from current-node
                        :distance d)]))
         (keys visited)))

(declare expand)

(defn start [g start-node target-node]
  (visualize/visualize-start g start-node target-node)
  #(expand g 0 start-node target-node {start-node nil} {}))

(defn visit [g target-node visited candidates]
  (let [[to from distance :as edge] (min-edge candidates)
        visited-result (conj visited edge)
        candidates-result (dissoc candidates to)]
    (visualize/visualize-visit g to from visited-result candidates-result distance)
    (cond
      (= to target-node) #(visualize/visualize-solution g visited-result target-node distance)
      (empty? candidates) #(visualize/visualize-fail g)
      :else #(expand g distance to target-node visited-result candidates-result))))

(defn expand [g distance current-node target-node visited candidates]
  (let [expanded-candidates (add-candidates g distance current-node target-node visited candidates)]
    (visualize/visualize-expand g visited candidates expanded-candidates)
    #(visit g target-node visited expanded-candidates)))

(defn shortest-path-step [g distance current-node target-node visited candidates]
  (let [candidates (add-candidates g distance current-node target-node visited candidates)]
    (let [[to from distance-result :as me] (min-edge candidates)
          v (conj visited me)]
      (cond
        ;;TODO: unroll
        (= to target-node) v
        (empty? candidates) "FAIL"
        ;; timeout
        :else (recur g distance-result to target-node v candidates)))))

(defn shortest-path [g start-node target-node searching?]
  ;;(prn "SP" (shortest-path-step g 0 start-node target-node {start-node nil} {}))
  (visualize/slow-trampoline 2000 searching? start g start-node target-node))

(task/tlet
  [])



;; TODO: edge/distance and distance is confusing
