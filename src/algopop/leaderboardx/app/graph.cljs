(ns algopop.leaderboardx.app.graph
  (:require [clojure.set :as set]
            [clojure.pprint]))

;; TODO: store ins as part of graph instead of recalculating
(defn in-edges [g k]
  (set
    (for [[from es] (:edges g)
          [to v] es
          :when (= k to)]
      from)))

(defn without-node [g id]
  (-> g
      (update-in [:nodes] dissoc id)
      (assoc :edges
             (into {}
                   (for [[k links] (dissoc (:edges g) id)]
                     [k (dissoc links id)])))))

(defn with-edge [g [from to]]
  (update-in g [:edges from] merge {to {}}))

(defn without-edge [g [from to]]
  (update-in g [:edges from] dissoc to))

(defn without-edges [g from tos]
  (apply update-in g [:edges from] dissoc tos))

;; TODO: make deep
(defn reverse-merge [& maps]
  (apply merge (reverse maps)))

(defn replace-in-edges [g ins k edge-type]
  (let [old-ins (in-edges g k)
        removals (set/difference old-ins ins)
        g (without-edges g k removals)]
    (reduce (fn collect-ins [acc in]
              (update-in acc [:edges in k] reverse-merge {:edge/type edge-type}))
            g
            ins)))

;; TODO: is nested map helping?
(defn replace-edges [g node-id node-type edge-type outs ins]
  (let [out-nodes (zipmap outs (repeat {:node/type node-type}))
        in-nodes (zipmap ins (repeat {:node/type node-type}))
        out-edges (zipmap outs (repeat {:edge/type edge-type}))]
    (-> g
        (update-in [:nodes] reverse-merge
                   {node-id {:node/type node-type}}
                   out-nodes
                   in-nodes)
        (update-in [:edges node-id] select-keys out-edges)
        (update-in [:edges node-id] reverse-merge out-edges)
        (replace-in-edges ins node-id edge-type))))

(defn rename-in-edges [g k new-k ins]
  (reduce (fn rebuild-edges [acc from]
            (-> acc
                (update-in [:edges from] dissoc k)
                (update-in [:edges from] reverse-merge {new-k {}})))
          g
          (keys ins)))

(defn rename-node [g k new-k]
  (if (= k new-k)
    g
    (let [node (get-in g [:nodes k])
          outs (get-in g [:edges k])
          ins (in-edges g k)]
      (-> g
          (update-in [:nodes] dissoc k)
          (update-in [:edges] dissoc k)
          (update-in [:nodes new-k] reverse-merge node)
          (update-in [:edges new-k] reverse-merge outs)
          (rename-in-edges k new-k ins)))))
