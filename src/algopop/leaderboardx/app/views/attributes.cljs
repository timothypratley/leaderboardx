(ns algopop.leaderboardx.app.views.attributes
  (:require [algopop.leaderboardx.app.firebase :as firebase]
            [algopop.leaderboardx.app.views.common :as common]))

(defn attributes-view []
  [firebase/on ["entities"]
   (fn [entities r]
     (prn "E" @entities)
     [:div
      [common/entity-editor
       "Entity"
       @entities
       "name"
       "name"
       (atom nil)
       (fn add-entity [v]
         (firebase/ref-push r {"name" v}))
       (fn remove-entity [id]
         (.remove (.child r id)))
       (fn add-attribute [id k]
         (prn "hi" id k)
         (.set (.child (.child r id) k) "hello"))
       (fn [id k]
         (.remove (.child (.child r id) k)))
       ]])])