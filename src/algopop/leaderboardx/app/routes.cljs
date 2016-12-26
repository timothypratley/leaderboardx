(ns algopop.leaderboardx.app.routes
  (:require [algopop.leaderboardx.app.views.about :as about]
            [algopop.leaderboardx.app.views.assess :as assess]
            [algopop.leaderboardx.app.views.coach :as coach]
            [algopop.leaderboardx.app.views.coach-player :as coach-player]
            [algopop.leaderboardx.app.views.details :as details]
            [algopop.leaderboardx.app.views.endorse :as endorse]
            [algopop.leaderboardx.app.views.graph-editor :as graph-editor]
            [algopop.leaderboardx.app.views.graph-table :as graph-table]
            [algopop.leaderboardx.app.views.settings :as settings]
            [bidi.bidi :as bidi]))

(def view
  {:about #'about/about-page
   :graph-editor #'graph-editor/graph-editor-page
   ;;:details #'details/details-view
   ;;:settings #'settings/settings-view
   ;;:coach-dashboard #'coach/coach-view
   ;;:coach-player #'coach-player/coach-player-view
   ;;:assessments #'assess/assessments-view
   ;;:assess #'assess/assess-view
   ;;:table #'graph-table/table-view
   ;;:endorse #'endorse/endorse-page
   })

;; TODO: can routes just be functions?
(def routes
  (let [ks (keys view)]
    ["/" (conj
          (mapv vector (map name ks) ks)
          ["assess/" {[:assessee "/" [keyword :type] "/" :date] :assess}])]))

(defn match [s]
  (bidi/match-route routes s))

(defn page [{:keys [handler route-params]}]
  [(view handler about/about-page) route-params])
