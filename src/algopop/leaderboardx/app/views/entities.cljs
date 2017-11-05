(ns algopop.leaderboardx.app.views.entities
  (:require
    [algopop.leaderboardx.app.firebase]
    [algopop.leaderboardx.app.views.common :as common]
    [algopop.leaderboardx.app.firebase :as firebase]
    [reagent.core :as reagent]))

;; TODO: created/modified aren't quite right
(defn entities-view []
  [firebase/on (firebase/user-entities-path)
   (fn [entities r]
     [:div
      [common/entity-editor
       "Entity"
       @entities
       (fn add-entity [id]
         (firebase/ref-update
           [r id]
           {"created" firebase/timestamp
            "name" id}))
       (fn remove-entity [id]
         (-> r (.child id) (.remove)))
       (fn add-attribute [id k v]
         (firebase/ref-update
           [r id]
           {k v
            "modified" firebase/timestamp}))
       (fn remove-attribute [id k]
         (firebase/ref-remove
           [r id k]))]])])
