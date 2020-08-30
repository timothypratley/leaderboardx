(ns algopop.leaderboardx.graph.graph-test
  (:require [clojure.test :refer [deftest is]]
            [algopop.leaderboardx.graph.graph :as graph])
  #?(:cljs
     (:require-macros [algopop.leaderboardx.graph.graph-test :refer [has]])))

#?(:clj
   (defmacro has
     ([actual sym form]
      `(has ~actual ~sym ~form nil))
     ([actual sym form msg]
      `(doto ~actual
         (as-> ~sym (is ~form ~msg))))))

(deftest graph-operations
  (-> (graph/create)
      (has g g)

      ;; Adding an edge
      (graph/with-edge "from" "to" "likes")
      (has g (= 1 (count (graph/edges g))))
      (has g (= 1 (graph/weight g ["from" "to"])))

      ;; Adding a node, in and out edges all at once
      (graph/replace-edges "Tim" "person" "likes" ["chocolate" "pie"] ["puppies" "sharks"])
      (has g (= 7 (count (graph/nodes g))))
      (has g (= 5 (count (graph/edges g))))
      (has g (= ["puppies" "sharks"] (keys (get (graph/in-edges g) "Tim"))))

      ;; Remove an out edge
      (graph/replace-edges "Tim" "person" "likes" ["chocolate"] ["puppies" "sharks"])
      (has g (= ["chocolate"] (keys (get (graph/out-edges g) "Tim"))))
      (has g (= 4 (count (graph/edges g))))

      ;; Remove an in edge
      (graph/replace-edges "Tim" "person" "likes" ["chocolate"] ["sharks"])
      (has g (= ["sharks"] (keys (get (graph/in-edges g) "Tim"))))
      (has g (= 3 (count (graph/edges g))))

      ;; Reciprocal edges
      (graph/with-edge "to" "from" "likes")
      (has g (= 3 (count (graph/edges-collapsed g))))
      (has g (= 4 (count (graph/edges g))))
      (has g (and (get (graph/out-edges g) "to")
                  (get (graph/out-edges g) "from")
                  (get (graph/in-edges g) "to")
                  (get (graph/in-edges g) "from")))))

(deftest graph-api
  (is (= 4
         (-> (graph/create)
             (graph/add-node "a" {:k 1})
             (atom)
             (swap! graph/update-attr "a" :k + 3)
             (graph/entity "a")
             :k))
      "Can update an attribute with a function"))
