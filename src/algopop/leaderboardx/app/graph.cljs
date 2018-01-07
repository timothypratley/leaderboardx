(ns algopop.leaderboardx.app.graph
  (:require
    [clojure.set :as set]
    [algopop.leaderboardx.app.pagerank :as pagerank]))

(defn with-ranks [g]
  (when-let [node-ids (seq (keys (:nodes g)))]
    (let [ranks (pagerank/ranks node-ids
                                ;; TODO: edges should just be the id->id; props in another spot
                                (into {}
                                      (for [[k m] (:edges g)
                                            :let [es (set (keys m))]]
                                        [k es])))]
      ;; TODO: be less riddic
      (update g :nodes #(merge-with merge %1 %2) (into {}
                                                       (for [[k pr r] ranks]
                                                         [k {:node/pagerank pr
                                                             :node/rank r}]))))))

;; TODO: store ins as part of graph instead of recalculating
(defn ^:private in-edges [g k edge-type]
  (set
    (for [[from es] (:edges g)
          [to {:keys [edge/type]}] es
          :when (and (= k to)
                     (or (nil? edge-type) (= type edge-type)))]
      from)))

(defn without-node [g id]
  (-> g
      (update-in [:nodes] dissoc id)
      (assoc :edges
             (into {}
                   (for [[k links] (dissoc (:edges g) id)]
                     [k (dissoc links id)])))
      (with-ranks)))

(defn without-edge [g from to]
  (prn from to g)
  (->
    (update-in g [:edges from] dissoc to)
    (doto (prn))
    (with-ranks)))

(defn ^:private without-in-edges [g k ins]
  (reduce (fn [acc from]
            (update-in acc [:edges from] dissoc k))
          g
          ins))

;; TODO: make deep
(defn ^:private reverse-merge [& maps]
  (apply merge (reverse maps)))

(defn ^:private replace-in-edges [g ins k edge-type]
  (let [old-ins (in-edges g k edge-type)
        removals (set/difference old-ins ins)
        g (without-in-edges g k removals)]
    (reduce (fn collect-ins [acc in]
              (update-in acc [:edges in k] reverse-merge {:edge/type edge-type}))
            g
            ins)))

(defn ^:private select-edges-to-keep [m edge-type out-edges]
  (into {}
        (filter
          (fn keep-edge [[k {:keys [edge/type]}]]
            (or
              (not= type edge-type)
              (out-edges k)))
          m)))

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
        (update-in [:edges node-id] select-edges-to-keep edge-type out-edges)
        (update-in [:edges node-id] reverse-merge out-edges)
        (replace-in-edges ins node-id edge-type)
        (with-ranks))))

(defn ^:private rename-in-edges [g k new-k ins]
  (reduce (fn rebuild-edges [acc from]
            (-> acc
                (update-in [:edges from] reverse-merge {new-k (get-in acc [:edges from k])})
                (update-in [:edges from] dissoc k)))
          g
          ins))

(defn rename-node [g k new-k]
  (if (= k new-k)
    g
    (let [node (get-in g [:nodes k])
          outs (get-in g [:edges k])
          ins (in-edges g k nil)]
      (-> g
          (update-in [:nodes] dissoc k)
          (update-in [:edges] dissoc k)
          (update-in [:nodes new-k] reverse-merge node)
          (update-in [:edges new-k] reverse-merge outs)
          (rename-in-edges k new-k ins)
          (with-ranks)))))

(defn update-edge [g from to e]
  (-> g
      (update-in [:edges from to] merge e)
      (with-ranks)))

(defn add-edge [g from to edge-type]
  (-> g
      (update-in [:edges from to] merge {:edge/type edge-type})
      (with-ranks)))

