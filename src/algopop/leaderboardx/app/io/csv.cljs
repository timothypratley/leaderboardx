(ns algopop.leaderboardx.app.io.csv
  (:require [algopop.leaderboardx.graph.graph :as graph]
            [algopop.leaderboardx.app.io.common :as common]
            [algopop.leaderboardx.app.logging :as log]
            [instaparse.core :as insta]
            [clojure.string :as string]))

(def csv-gramma
  "csv : [row] {<eol> row}
row : field {<','> field}
<field> : [<ws>] id [<ws>]
ws : #'\\s*'
eol : '\n' | '\r\n' | '\n\r'
<id> : literal | numeral | quoted | html
<literal> : #'[a-zA-Z\\200-\\377][a-zA-Z\\200-\\377\\_0-9 ]*'
<numeral> : #'[-]?(.[0-9]+|[0-9]+(.[0-9]*)?)'
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<html> : #'<[^>]*>'")

(def parse-csv
  (insta/parser csv-gramma))

(defn collect-row [g [_ from & tos]]
  (if (seq tos)
    (graph/with-successors g from tos "likes")
    (graph/add-node g from {:node/type "person"})))

(defn read-graph [csv]
  (let [ast (parse-csv csv)]
    (if (insta/failure? ast)
      (log/error ast "Failed to parse CSV")
      (reduce collect-row (graph/create) (nnext ast)))))

(defn write-graph [g]
  (let [outs (graph/out-edges g)]
    (str
      (string/join
        \newline
        (cons
          (string/join "," (map common/quote-escape
                                ["Person" "Endorses"]))
          (for [[k attrs] (sort (graph/nodes g))]
            (string/join
              ","
              (map common/quote-escape
                   (cons k (sort (get outs k))))))))
      \newline)))
