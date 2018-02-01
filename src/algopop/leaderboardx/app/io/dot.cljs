(ns algopop.leaderboardx.app.io.dot
  (:require [algopop.leaderboardx.app.io.common :as common]
            [algopop.leaderboardx.app.logging :as log]
            [algopop.leaderboardx.graph.graph :as graph]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [instaparse.core :as insta]))

(def dot-gramma
  "graph : <ws> [<'strict'> <ws>] ('graph' | 'digraph') <ws> [id <ws>] <'{'> stmt_list <'}'> <ws>
<stmt_list> : <ws> (stmt <ws> [<';'> <ws>])*
<stmt> : node | edge | attr | eq | subgraph
eq : id <ws> <'='> <ws> id
attr : ('graph' | 'node' | 'edge') <ws> attr_list
<attr_list> : (<'['> <ws> [a_list <ws>] <']'> <ws>)+
<a_list> : a (<(';' | ',')> <ws> a)*
<a> : id <ws> <'='> <ws> id <ws>
edge : (node_id | subgraph) <ws> edge_RHS [<ws> attr_list]
<edge_RHS> : (<edge_op> <ws> (node_id | subgraph) <ws>)+
edge_op : '->' | '--'
node : node_id [<ws> attr_list]
<node_id> : id [<ws> port]
port  : <':'> <ws> id [<ws> <':'> <ws> compass_pt] | <':'> <ws> compass_pt
compass_pt  : 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_'
subgraph  : ['subgraph' [<ws> id] <ws>] <'{'> <ws> stmt_list <ws> <'}'>
ws : #'\\s*'
<id> : literal | number | quoted | html
<literal> : #'[a-zA-Z\\200-\\377][a-zA-Z\\200-\\377\\_\\d]*'
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<number> : #'-?([\\.]\\d+|\\d+(\\.\\d*)?)'
<html> : #'<[^>]*>'")

(def parse-dot
  (insta/parser dot-gramma))

(defn qualify-keywords [m q]
  (into {}
        (for [[k v] m]
          [(keyword q (name k)) v])))

(defn collect-statement [graph [statement-type & statement-body]]
  (condp = statement-type
    :node (let [[id & attrs] statement-body
                attr-map (qualify-keywords (apply hash-map attrs) "node")]
            (graph/add-node graph id attr-map))
    :edge (let [[from to & attrs] statement-body
                attr-map (qualify-keywords (apply hash-map attrs) "edge")]
            (graph/add-edge graph [from to] attr-map))
    :attr (merge graph (qualify-keywords (apply hash-map statement-body) "graph"))
    :eq graph
    :subgraph graph
    graph))

(defn read-graph [s]
  (let [ast (parse-dot s)]
    (if (insta/failure? ast)
      (log/error ast "Failed to parse dot file")
      (let [[_ _ & statements] ast
            [title] statements
            statements (if (string? title)
                         (rest statements)
                         statements)
            graph (cond-> (graph/create {})
                          (string? title) (assoc :title title))]
        (reduce collect-statement graph statements)))))

(defn maybe-attrs [attrs]
  (when (seq attrs)
    (str " ["
         (string/join ", " (for [[k v] attrs]
                            (str (name k) " = " (pr-str v))))
         "]")))

(defn edges [g]
  (for [[[from to] attrs] (sort (walk/stringify-keys (graph/edges g)))]
    (str (common/quote-escape from)
         " -> "
         (common/quote-escape to)
         (maybe-attrs attrs)
         ";")))

(defn nodes [g]
  (for [[k attrs] (sort (walk/stringify-keys (graph/nodes g)))]
    (str (common/quote-escape k) (maybe-attrs attrs) ";")))

;; TODO: why do sometimes ranks exist, sometimes not? not merging?
(defn write-graph [g]
  (str "digraph " (common/quote-escape (:title g "untitled")) " {" \newline
       (string/join \newline
         (concat
           (nodes g)
           (edges g)))
       \newline "}"))
