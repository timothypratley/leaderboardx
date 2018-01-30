(ns algopop.leaderboardx.graph.graph
  (:require
    [clojure.set :as set]
    [algopop.leaderboardx.graph.pagerank :as pagerank]
    [loom.graph :as lg]
    [loom.attr :as la]))

(defn add-attr [g id k v]
  (la/add-attr g id k v))

(defn weight [g edge]
  (lg/weight g edge))

(defn add-attrs [g [id attrs]]
  (reduce
    (fn [acc2 [k v]]
      (la/add-attr acc2 id k v))
    g
    attrs))

(defn add-weight [g [a b] w]
  (assoc-in g [:adj a b] w))

(defn edge-attrs [g edges]
  (reduce add-attrs g edges))

;; TODO: with-ranks
(defn create [edges]
  (edge-attrs
    (apply lg/weighted-digraph (keys edges))
    edges))

(defn nodes [g]
  (into {}
        (for [node-id (lg/nodes g)]
          [node-id (or (la/attrs g node-id) {})])))

(defn edges [g]
  (into {}
        (for [edge (lg/edges g)]
          [edge (assoc (or (la/attrs g edge) {})
                  ;; TODO: hmmm not sure I like just overwritting it but meh
                  :edge/weight (lg/weight g edge))])))

(defn out-edges
  ([g]
   (out-edges g (constantly true)))
  ([g filter-fn]
   (reduce (fn [acc [[from to] v]]
             (if (filter-fn v)
               (update-in acc [from to] v)
               acc))
           {}
           (edges g))))

(defn in-edges
  ([g]
   (in-edges g (constantly true)))
  ([g filter-fn]
   (reduce (fn [acc [[from to] v]]
             (if (filter-fn v)
               (update-in acc [to from] v)
               acc))
           {}
           (edges g))))

(defn with-ranks [g]
  g
  #_(when-let [node-ids (seq (keys (:nodes g)))]
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

(defn update-edge [g edge-id k v]
  (-> g
      (la/add-attr edge-id k v)
      (with-ranks)))

(defn with-edge [g from to edge-type]
  (-> g
      (lg/add-edges [from to])
      (la/add-attr [from to] :edge/type edge-type)
      (with-ranks)))

(defn without-node [g id]
  (-> g
      (lg/remove-nodes g id)
      (with-ranks)))

(defn without-edge [g from to]
  (->
    (lg/remove-edges g [from to])
    (with-ranks)))

(defn ^:private without-in-edges [g node-id ins]
  (lg/remove-edges* g (for [from ins]
                        [from node-id])))

;; TODO: make deep
(defn ^:private reverse-merge [& maps]
  (apply merge (reverse maps)))

(defn with-successors [g node-id outs edge-type]
  (reduce (fn collect-ins [acc out]
            (with-edge acc node-id out edge-type))
          g
          outs))

(defn with-predecessors [g node-id ins edge-type]
  (reduce (fn collect-ins [acc in]
            (with-edge acc in node-id edge-type))
          g
          ins))

(defn ^:private replace-out-edges [g outs node-id edge-type]
  (let [old-ins (filter #(= edge-type (la/attr g % :edge/type)) (lg/successors g node-id))
        removals (set/difference old-ins outs)
        g (without-in-edges g node-id removals)]
    (with-successors g node-id outs edge-type)))

(defn ^:private replace-in-edges [g ins node-id edge-type]
  (let [old-ins (filter #(= edge-type (la/attr g % :edge/type)) (lg/predecessors g node-id))
        removals (set/difference old-ins ins)
        g (without-in-edges g node-id removals)]
    (with-predecessors g node-id ins edge-type)))

(defn replace-edges [g node-id node-type edge-type outs ins]
  (let [node-ids (concat [node-id] outs ins)]
    (-> g
        (lg/add-nodes* node-ids)
        (la/add-attr-to-nodes :node/type node-type node-ids)
        (replace-in-edges ins node-id edge-type)
        (replace-out-edges outs node-id edge-type)
        (with-ranks))))

(defn rename-node [g node-id new-node-id]
  (if (= node-id new-node-id)
    g
    ;; TODO: edge attrs
    (let [node-attrs (la/attrs g node-id)
          outs (lg/successors g node-id)
          ins (lg/predecessors g node-id)
          ;; TODO: fix me
          edge-type "likes"]
      (-> g
          (lg/remove-nodes node-id)
          (lg/add-nodes new-node-id)
          (add-attrs [new-node-id node-attrs])
          (with-successors new-node-id outs edge-type)
          (with-predecessors new-node-id ins edge-type)
          (with-ranks)))))

;; TODO: fix io
;; TODO: when renaming nodes, the edge attrs might be left over?
