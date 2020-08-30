(ns algopop.leaderboardx.app.views.common-test
  (:require [algopop.leaderboardx.app.views.common :as common]
            [devcards.core :as dc :refer-macros [defcard-rg deftest]]))

(defcard-rg editable-string-example
  [common/editable-string "foo"])

(defcard-rg entity-editor-example
  [common/entity-editor
   "Students"
   {"sally" {:person/name "sally" :person/birth-date "1999-01-01" :student/number "101"}
    "suzie" {:person/name "suzie" :person/birth-date "1999-02-02" :student/number "102"}}])
