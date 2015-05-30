(ns algopop.leaderboardx.app.io.dot
  (:require [algopop.leaderboardx.app.io.common :as common]
            [algopop.leaderboardx.app.logging :as log]
            [clojure.string :as string]
            [instaparse.core :as insta]))

(def dot-gramma
  "graph : <ws> [ <'strict'> <ws> ] ('graph' | 'digraph') <ws> [ id <ws> ] <'{'> stmt_list <'}'> <ws>
<stmt_list> : <ws> [ stmt <ws> [ <';'> ] [ stmt_list ] <ws> ]
<stmt>  : node | edge | attr | eq | subgraph
eq : id <ws> <'='> <ws> id
attr : ('graph' | 'node' | 'edge') <ws> attr_list
<attr_list> : <'['> <ws> [ a_list <ws>] <']'> [ <ws> attr_list ]
<a_list>  : id <ws> <'='> <ws> id [ <ws> <(';' | ',')> ] [ <ws> a_list ]
edge : (node_id | subgraph) <ws> edge_RHS [ <ws> attr_list ]
<edge_RHS> : <edge_op> <ws> (node_id | subgraph) [ <ws> edge_RHS ]
edge_op : '->' | '--'
node : node_id [ <ws> attr_list ]
<node_id> : id [ <ws> port ]
port  : <':'> <ws> id [ <ws> <':'> <ws> compass_pt ] | <':'> <ws> compass_pt
compass_pt  : ('n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_')
subgraph  : [ subgraph [ <ws> id ] <ws> ] <'{'> <ws> stmt_list <ws> <'}'>
ws : #'\\s*'
<id> : #'[a-zA-Z\\200-\\377][a-zA-Z\\200-\\377\\_0-9]*' | numeral | quoted | html
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<numeral> : #'[-]?(.[0-9]+|[0-9]+(.[0-9]*)?)'
<html> : #'<[^>]*>'")

(def parse-dot
  (insta/parser dot-gramma))

(defn collect-statement [graph [statement-type & statement-body]]
  (condp = statement-type
    :node (let [[id & attrs] statement-body
                attr-map (apply hash-map attrs)]
            (assoc-in graph [:nodes id] attr-map))
    :edge (let [[from to & attrs] statement-body
                attr-map (apply hash-map attrs)]
            (-> graph
                (assoc-in [:edges from to] attr-map)
                (update-in [:nodes from] merge {})
                (update-in [:nodes to] merge {})))
    :attr (merge (apply hash-map statement-body) graph)
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
            graph (if (string? title)
                    {:title title}
                    {})]
        (reduce collect-statement graph statements)))))

(defn maybe-attrs [attrs]
  (when (seq attrs)
    (str " ["
         (string/join "," (for [[k v] attrs]
                            (str (common/quote-escape (name k))
                                 " = " (common/quote-escape (pr-str v)))))
         "]")))

(defn edges [g]
  (for [[from tos] (sort-by key (:edges g))
        [to attrs] (sort-by key tos)]
    (str (common/quote-escape from)
         " -> "
         (common/quote-escape to)
         (maybe-attrs attrs)
         ";")))

(defn nodes [g]
  (for [[k attrs] (sort-by (comp :rank val) (:nodes g))]
    (str k (maybe-attrs attrs) ";")))

;; TODO: why do sometimes ranks exist, sometimes not? not merging?
(defn write-graph [g]
  (str "digraph " (common/quote-escape (:title g)) " {" \newline
       (string/join \newline
                    (concat
                     (nodes g)
                     (edges g)))
       \newline "}"))
