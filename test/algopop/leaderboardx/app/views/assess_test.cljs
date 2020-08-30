(ns algopop.leaderboardx.app.views.assess-test
  (:require [algopop.leaderboardx.app.views.assess :as assess]
            [devcards.core :refer [defcard-rg defcard deftest]]
            [clojure.test :refer [is]]))

(deftest titles-test
  (is (= :assessment/group-hug-please
         (assess/title2attr "Group hug please")))
  (is (= "Group hug please"
         (assess/attr2title :assessment/group-hug-please))))

#_(defcard-rg assessment-card
  [assess/assessment-example])
