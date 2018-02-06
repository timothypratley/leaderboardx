(ns algopop.leaderboardx.graph.graph-test
  (:require
    [clojure.test :refer :all]
    [algopop.leaderboardx.graph.graph :as graph]))

(defmacro has
  ([actual sym form]
   `(has ~actual ~sym ~form nil))
  ([actual sym form msg]
   `(doto ~actual
      (as-> ~sym (is ~form ~msg)))))

(deftest graph-operations
  (-> (graph/create)
      (has g g)
      (graph/with-edge "from" "to" "likes")
      (has g (= 1 (count (graph/edges g))))
      (has g (= 1 (graph/weight g ["from" "to"])))
      (graph/replace-edges "Tim" "person" "likes" ["chocolate" "pie"] ["puppies" "sharks"])
      (has g (= 7 (count (graph/nodes g))))
      (has g (= 5 (count (graph/edges g))))
      (has g (= ["puppies" "sharks"] (keys (get (graph/in-edges g) "Tim"))))))
