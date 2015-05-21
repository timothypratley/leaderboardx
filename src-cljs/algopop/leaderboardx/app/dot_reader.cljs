(ns algopop.leaderboardx.app.dot-reader
  (:require [instaparse.core :as insta]))

(def dot-gramma
  "graph : <WS> [ 'strict' <WS> ] ('graph' | 'digraph') <WS> [ id <WS> ] <'{'> stmt_list <'}'> <WS>
<stmt_list> : <WS> [ stmt <WS> [ <';'> ] [ stmt_list ] <WS> ]
<stmt>  : node_stmt | edge_stmt | attr_stmt | eq_stmt | subgraph
eq_stmt : id <WS> <'='> <WS> id
attr_stmt : ('graph' | 'node' | 'edge') <WS> attr_list
attr_list : <'['> <WS> [ a_list <WS>] <']'> [ <WS> attr_list ]
a_list  : id <WS> <'='> <WS> id [ <WS> <(';' | ',')> ] [ <WS> a_list ]
edge_stmt : (node_id | subgraph) <WS> edgeRHS [ <WS> attr_list ]
<edgeRHS> : <edgeop> <WS> (node_id | subgraph) [ <WS> edgeRHS ]
edgeop : '->' | '--'
node_stmt : node_id [ <WS> attr_list ]
node_id : id [ <WS> port ]
port  : <':'> <WS> id [ <WS> <':'> <WS> compass_pt ] | <':'> <WS> compass_pt
compass_pt  : ('n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_')
subgraph  : [ subgraph [ <WS> id ] <WS> ] <'{'> <WS> stmt_list <WS> <'}'>
WS : #'\\s*'
<id> : #'[a-zA-Z\\200-\\377\\_\\-\\.\\/\\\"0-9]+'")

(def parse-dot
  (insta/parser dot-gramma :output-format :hiccup))

(defn test-dot []
  (parse-dot "digraph deps {\"dev/browser-repl\" -> \"cemerick.piggieback/cljs-repl\";
\"dev/start-figwheel\" -> \"leiningen.core.main/-main\";
\"dev/browser-repl\" -> \"weasel.repl.websocket/repl-env\";
\"dev/browser-repl\"[label=\"dev/browser-repl\"];
\"weasel.repl.websocket/repl-env\"[label=\"weasel.repl.websocket/repl-env\"];
\"dev/start-figwheel\"[label=\"dev/start-figwheel\"];
\"leiningen.core.main/-main\"[label=\"leiningen.core.main/-main\"];
\"cemerick.piggieback/cljs-repl\"[label=\"cemerick.piggieback/cljs-repl\"];}"))

(println "DOT:" (test-dot))
