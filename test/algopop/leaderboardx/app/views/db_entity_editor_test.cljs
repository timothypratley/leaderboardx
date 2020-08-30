(ns algopop.leaderboardx.app.views.db-entity-editor-test
  (:require [algopop.leaderboardx.app.views.db-entity-editor :as ee]
            [devcards.core :refer [defcard-rg]]))

(defcard-rg entity-editor3-example
  [ee/db-entity-editor "person" "People" '{:person/name _}])
