(ns algopop.leaderboardx.app.routes
  (:require
    [algopop.leaderboardx.app.views.about :as about]
    [algopop.leaderboardx.app.views.assess :as assess]
    [algopop.leaderboardx.app.views.coach :as coach]
    [algopop.leaderboardx.app.views.coach-player :as coach-player]
    [algopop.leaderboardx.app.views.details :as details]
    [algopop.leaderboardx.app.views.endorse :as endorse]
    [algopop.leaderboardx.app.views.entities :as entities]
    [algopop.leaderboardx.graph.graph-editor :as graph-editor]
    [algopop.leaderboardx.graph.graph-table :as graph-table]
    [algopop.leaderboardx.app.views.graph-list :as graph-list]
    [algopop.leaderboardx.app.views.roster :as roster]
    [algopop.leaderboardx.app.views.settings :as settings]
    [algopop.leaderboardx.app.views.schema :as schema]
    [bidi.bidi :as bidi]
    [reagent.session :as session]
    [algopop.leaderboardx.app.firebase :as firebase]))

(def routes
  ["/"
   [["about" #'about/about-page]
    ;;["schema" #'schema/schema-view]
    ;;["entities" #'entities/entities-view]
    #_["graphs" {"" #'graph-list/graph-list-view
               ["/" :id] #'graph-editor/graph-editor-page}]
    ;;    ["graph" {"" #'graph-editor/graph-editor-page}]
    ["graph-editor" {"" #'graph-editor/graph-editor-page}]
    ["forum" #'about/forum-page]
    ["roster" #'roster/roster-page]
    #_["assessments" {"" #'assess/assessments-view
                    ["/" :assessee "/" [keyword :type] "/" :date] #'assess/assess-view}]
    #_["coach" #'coach/coach-view]]])

(def links
  (mapv first (second routes)))

(defn match [s]
  (bidi/match-route routes s))

(defn navigate [event]
  (session/assoc-in! [:viewpoint :route] (.-token event)))

(defn current-page []
  (let [{:keys [handler route-params]} (match (session/get-in [:viewpoint :route]))]
    [(or handler about/about-page) route-params]))
