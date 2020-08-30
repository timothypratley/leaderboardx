(ns algopop.leaderboardx.app.views.schema-test
  (:require [algopop.leaderboardx.app.views.types :as schema]
            [devcards.core :refer [defcard-rg]]))

(defcard-rg schema-example
  "TEST"
  [schema/types-view])
