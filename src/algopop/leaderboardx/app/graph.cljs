(ns algopop.leaderboardx.app.graph
  (:require [algopop.leaderboardx.app.pagerank :as pagerank]
            [clojure.set :as set]
            [algopop.leaderboardx.app.db :as db]))

;; TODO: store ins as part of graph instead of recalculating
(defn in-edges [g k]
  (into {}
   (for [[from es] (:edges g)
         [to v] es
         :when (= k to)]
     [from to])))

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

(defn without-edges [g from & tos]
  (apply update-in g [:edges from] dissoc tos))

;; TODO: make deep
(defn merge-left [& maps]
  (apply merge (reverse maps)))

(defn replace-in-edges [g ins k]
  (let [old-ins (in-edges g k)
        removals (set/difference (set old-ins) (set ins))]
    (reduce (fn collect-ins [acc in]
              (update-in acc [:edges in k] merge-left {}))
            (without-edges g k removals)
            ins)))

(defn replace-edges [g k outs ins]
  (-> g
      (update-in [:nodes] merge-left {k {}} (zipmap outs (repeat {})) (zipmap ins (repeat {})))
      (update-in [:edges k] merge-left (zipmap outs (repeat {})))
      (replace-in-edges ins k)))

(defn rename-in-edges [g k new-k ins]
  (reduce (fn rebuild-edges [acc from]
            (-> acc
                (update-in [:edges from] dissoc k)
                (update-in [:edges from] merge-left {new-k {}})))
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
          (update-in [:nodes new-k] merge-left node)
          (update-in [:edges new-k] merge-left outs)
          (rename-in-edges k new-k ins)))))
