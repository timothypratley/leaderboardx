(ns algopop.leaderboardx.app.views.graph-list
  (:require
    [algopop.leaderboardx.app.firebase :as firebase]
    [algopop.leaderboardx.app.views.common :as common]
    [reagent.core :as reagent]))

(def editing
  (reagent/atom nil))

#_(defn graph-list-view []
  [firebase/on ["graphs"]
   (fn [graphs r]
     [:div
      [common/entity-editor "Graphs" @graphs
       editing
       (fn [])
       (fn [])]])])

(defn graph-list-view []
  [firebase/on ["entities"]
   (fn [sets r]
     [:div
      [common/entity-editor
       "Sets"
       @sets
       editing
       (fn add-entity [id]
         (firebase/ref-update
           [r id]
           {"created" firebase/timestamp
            "name" id
            "type" "set"}))
       (fn remove-entity [id]
         (-> r (.child id) (.remove)))
       (fn add-attribute [id k v]
         (firebase/ref-update
           [r id]
           {k v
            "modified" firebase/timestamp}))
       (fn remove-attribute [id k]
         (firebase/ref-remove
           [r id k]))]])
   (fn [r]
     (-> r
         (.orderByChild "type")
         (.equalTo "set")))])