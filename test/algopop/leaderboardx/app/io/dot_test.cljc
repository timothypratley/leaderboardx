(ns algopop.leaderboardx.app.io.dot-test
  (:require [clojure.test :refer :all]
            [algopop.leaderboardx.app.io.dot :as dot]
            [algopop.leaderboardx.graph.seed :as seed]))

(deftest read-write-graph-test
  (let [g (seed/example-graph)]
    (is (= g
           (dot/read-graph (dot/write-graph g))))))
