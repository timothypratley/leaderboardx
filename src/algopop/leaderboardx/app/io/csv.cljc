(ns algopop.leaderboardx.app.io.csv
  (:require [algopop.leaderboardx.graph.graph :as graph]
            [algopop.leaderboardx.app.io.common :as common]
            #?(:cljs [algopop.leaderboardx.app.logging :as log])
            [instaparse.core :as insta]
            [clojure.string :as string]))

(def csv-gramma
  "csv : [row] {<eol> row}
row : field {<','> field}
<field> : [<ws>] id [<ws>]
ws : #'\\s*'
eol : '\n' | '\r\n' | '\n\r'
<id> : literal | numeral | quoted | html
<literal> : #'[a-zA-Z200-377][a-zA-Z200-377\\_0-9 ]*'
<numeral> : #'[-]?(.[0-9]+|[0-9]+(.[0-9]*)?)'
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<html> : #'<[^>]*>'")

(def parse-csv
  (insta/parser csv-gramma))

(defn by-type [acc current-type [x & more]]
  (if x
    (if (graph/edge-types x)
      (recur acc x more)
      (recur (update acc current-type conj x) current-type more))
    acc))

(defn add-node-types [g tos]
  (reduce
    (fn [acc to]
      (graph/add-node acc to {}))
    g
    tos))

(defn with-edges [g from tos]
  (reduce
    (fn [acc [edge-type entities]]
      (-> acc
        (add-node-types entities)
        (graph/with-successors from entities edge-type)))
    g
    (by-type {} "likes" tos)))

(defn collect-row [g [_ from & tos]]
  (-> g
    (graph/add-node from {:node/type "person"})
    (with-edges from tos)))

(defn read-graph [csv]
  (let [ast (parse-csv csv)]
    (if (insta/failure? ast)
      #?(:cljs (log/error ast "Failed to parse CSV")
         :clj (println ast "Failed to parse CSV"))
      (reduce collect-row (graph/create) (nnext ast)))))

(defn with-types [edges]
  (reduce
    (fn [acc [edge-type es]]
      (concat
        acc
        (cond->> (sort (keys es))
                 edge-type (cons edge-type))))
    ()
    (sort (group-by (comp :edge/type val) edges))))

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
                   (cons k (with-types (get outs k))))))))
      \newline)))
