(ns algopop.leaderboardx.app.io.csv
  (:require [algopop.leaderboardx.app.graph :as graph]
            [algopop.leaderboardx.app.io.common :as common]
            [instaparse.core :as insta]
            [clojure.string :as string]))

(def csv-gramma
  "csv : { row }
row : field_list <eol>
<field_list> : field [ <','> field_list ]
<field> : [ <ws> ] id [ <ws> ]
ws : #'\\s*'
eol : '\n' | '\r\n' | '\n\r'
<id> : #'[a-zA-Z\\200-\\377][a-zA-Z\\200-\\377\\_0-9]*' | numeral | quoted | html
<numeral> : #'[-]?(.[0-9]+|[0-9]+(.[0-9]*)?)'
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<html> : #'<[^>]*>'")

(def parse-csv
  (insta/parser csv-gramma))

(defn collect-row [g [_ from & tos]]
  (-> g
      (assoc-in [:nodes from] {})
      (assoc-in [:edges from] (zipmap tos (repeat {})))))

(defn read-graph [csv]
  (let [ast (parse-csv csv)
        [_ & rows] ast]
    (if (insta/failure? ast)
      (prn "FAILURE" ast) ; TODO: put into global state
      (reduce collect-row {} (rest rows)))))

(defn write-graph [g]
  (str
   (string/join
    \newline
    (cons
     "\"Person\", \"Endorses\", \"Endorsed by\""
     (for [[k attrs] (sort-by (comp :rank val) (:nodes g))]
       (string/join
        ","
        (map common/quote-escape
             (cons k (sort (keys (get-in g [:edges k])))))))))
   \newline))
