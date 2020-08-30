(ns algopop.leaderboardx.app.io.csv-test
  (:require [clojure.test :refer [deftest is]]
            [algopop.leaderboardx.app.io.csv :as csv]
            [algopop.leaderboardx.graph.seed :as seed]))

(deftest read-write-graph-test
  (let [g (seed/basic-example-graph)]
    (is (= g
           (csv/read-graph (csv/write-graph g))))))
