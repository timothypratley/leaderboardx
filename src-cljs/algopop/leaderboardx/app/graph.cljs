(ns algopop.leaderboardx.app.graph
  (:require [algopop.leaderboardx.app.pagerank :as pagerank]))

(defn in-edges [g k]
  (distinct
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

(defn without-edge [g [from to]]
  (update-in g [:edges from] dissoc to))

(defn matrix-with-link [acc [to from]]
  (assoc-in acc [to from] 1))

(defn graph->matrix [g ks]
  (let [n (count ks)
        k->idx (zipmap ks (range))
        matrix (vec (repeat n (vec (repeat n 0))))]
    (reduce matrix-with-link matrix
            (for [[from es] (:edges g)
                  [to v] es]
              [(k->idx to) (k->idx from)]))))

(defn rank [g]
  (let [ks (keys (:nodes g))
        prs (pagerank/pagerank (graph->matrix g ks))]
    (reverse (sort-by second (map vector ks prs)))))

(defn with-rank [[g prev-rank prev-score] [k pagerank]]
  (let [rank (if (= pagerank prev-score)
               prev-rank
               (inc prev-rank))]
    [(-> g
         (assoc-in [:nodes k :rank] rank)
         (assoc-in [:nodes k :pagerank] pagerank))
     rank
     pagerank]))

(defn with-ranks
  "Calculates the pagerank and adds it to a graph.
  Takes a map of nodes and edges.
  Returns a map of nodes and edges."
  [g]
  (let [ranks (rank g)]
    (println "RANKS" ranks)
    (first (reduce with-rank [g 0 0] ranks))))
