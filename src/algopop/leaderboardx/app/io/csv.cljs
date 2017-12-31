(ns algopop.leaderboardx.app.io.csv
  (:require [algopop.leaderboardx.app.graph :as graph]
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
  (-> g
      (update-in [:nodes] graph/reverse-merge (zipmap tos (repeat {})))
      (assoc-in [:nodes from] {})
      (assoc-in [:edges from] (zipmap tos (repeat {})))))

(defn read-graph [csv]
  (let [ast (parse-csv csv)]
    (if (insta/failure? ast)
      (log/error ast "Failed to parse CSV")
      (reduce collect-row {} (nnext ast)))))

(defn write-graph [g]
  (str
   (string/join
    \newline
    (cons
     (string/join "," (map common/quote-escape
                           ["Person" "Endorses"]))
     (for [[k attrs] (sort-by (comp :rank val) (:nodes g))]
       (string/join
        ","
        (map common/quote-escape
             (cons k (sort (keys (get-in g [:edges k])))))))))
   \newline))
