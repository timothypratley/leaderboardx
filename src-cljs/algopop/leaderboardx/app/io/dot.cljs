(ns algopop.leaderboardx.app.io.dot
  (:require [instaparse.core :as insta]))

(def dot-gramma
  "graph : <WS> [ <'strict'> <WS> ] ('graph' | 'digraph') <WS> [ id <WS> ] <'{'> stmt_list <'}'> <WS>
<stmt_list> : <WS> [ stmt <WS> [ <';'> ] [ stmt_list ] <WS> ]
<stmt>  : node | edge | attr | eq | subgraph
eq : id <WS> <'='> <WS> id
attr : ('graph' | 'node' | 'edge') <WS> attr_list
<attr_list> : <'['> <WS> [ a_list <WS>] <']'> [ <WS> attr_list ]
<a_list>  : id <WS> <'='> <WS> id [ <WS> <(';' | ',')> ] [ <WS> a_list ]
edge : (node_id | subgraph) <WS> edgeRHS [ <WS> attr_list ]
<edgeRHS> : <edgeop> <WS> (node_id | subgraph) [ <WS> edgeRHS ]
edgeop : '->' | '--'
node : node_id [ <WS> attr_list ]
<node_id> : id [ <WS> port ]
port  : <':'> <WS> id [ <WS> <':'> <WS> compass_pt ] | <':'> <WS> compass_pt
compass_pt  : ('n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_')
subgraph  : [ subgraph [ <WS> id ] <WS> ] <'{'> <WS> stmt_list <WS> <'}'>
WS : #'\\s*'
<id> : #'[a-zA-Z\\200-\\377][a-zA-Z\\200-\\377\\_0-9]*' | numeral | quoted | html
<numeral> : #'[-]?(.[0-9]+|[0-9]+(.[0-9]*)?)'
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<html> : #'<[^>]*>'")

(def parse-dot
  (insta/parser dot-gramma))

(defn collect [graph [statement-type & statement-body]]
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
      (prn "FAILURE" ast)
      (let [[_ _ & statements] ast
            [title] statements
            statements (if (string? title)
                         (rest statements)
                         statements)
            graph (if (string? title)
                    {:title title}
                    {})]
        (reduce collect graph statements)))))

(def dot
  "digraph deps {
\"dev/browser-repl\" -> \"cemerick.piggieback/cljs-repl\";
\"dev/start-figwheel\" -> \"leiningen.core.main/-main\";
\"dev/browser-repl\" -> \"weasel.repl.websocket/repl-env\";
\"dev/browser-repl\"[label=\"dev/browser-repl\"];
\"weasel.repl.websocket/repl-env\"[label=\"weasel.repl.websocket/repl-env\"];
\"dev/start-figwheel\"[label=\"dev/start-figwheel\"];
\"leiningen.core.main/-main\"[label=\"leiningen.core.main/-main\"];
\"cemerick.piggieback/cljs-repl\"[label=\"cemerick.piggieback/cljs-repl\"];}")

#_(prn "PARSE:" (parse-dot dot))
#_(prn "READ:" (read-graph dot))

(defn write-graph [g]
  (str "digraph " (:title g) " {"
       "}"))
