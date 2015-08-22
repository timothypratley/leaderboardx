(ns algopop.leaderboardx.app.pages
  (:require [algopop.leaderboardx.app.views.about :as about]
            [algopop.leaderboardx.app.views.assess :as assess]
            [algopop.leaderboardx.app.views.coach :as coach]
            [algopop.leaderboardx.app.views.coach-player :as coach-player]
            [algopop.leaderboardx.app.views.details :as details]
            [algopop.leaderboardx.app.views.endorse :as endorse]
            [algopop.leaderboardx.app.views.graph-editor :as graph-editor]
            [algopop.leaderboardx.app.views.home :as home]
            [algopop.leaderboardx.app.views.schema :as schema]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]))

(def page
  {;:home #'home/home-page
   :about #'about/about-page
   :graph-editor #'graph-editor/graph-editor-page
   :details #'details/details-view
   ;;:schema #'schema/schema-view
   ;:coach-dashboard #'coach/coach-view
   ;:coach-player #'coach-player/coach-player-view
   :assess #'assess/assess-view
   ;:endorse #'endorse/endorse-page
   })

(secretary/set-config! :prefix "#")

;; TODO: handle params
(doseq [[k v] page]
  (secretary/defroute (str "/" (name k)) []
    (session/assoc-in! [:viewpoint :current-page] k)))

(secretary/defroute "*" []
  (session/assoc-in! [:viewpoint :current-page] :graph-editor))
