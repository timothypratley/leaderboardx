(ns algopop.leaderboardx.app.pages
  (:require [algopop.leaderboardx.app.views.about :as about]
            [algopop.leaderboardx.app.views.endorse :as endorse]
            [algopop.leaderboardx.app.views.graph-editor :as graph-editor]
            [algopop.leaderboardx.app.views.home :as home]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]))

(def page
  {;:home home/home-page
   :about about/about-page
   :graph-editor graph-editor/graph-editor-page
   ;:endorse endorse/endorse-page
   })

(secretary/set-config! :prefix "#")

;; TODO: handle params
(doseq [[k v] page]
  (secretary/defroute (str "/" (name k)) []
    (session/assoc-in! [:viewpoint :current-page] k)))

(secretary/defroute "*" []
  (session/assoc-in! [:viewpoint :current-page] :graph-editor))
