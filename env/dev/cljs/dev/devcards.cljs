(ns dev.devcards
  (:require
   [algopop.leaderboardx.app.views.common :as common]
   [algopop.leaderboardx.app.views.assess :as assess]
   [devcards.core :as dc :refer-macros [defcard]]
   [reagent.core :as reagent]))

(enable-console-print!)

(defcard
  "# Test")
