(ns algopop.leaderboardx.app.views.types
  (:require [algopop.leaderboardx.app.firebase :as firebase]
            [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.views.db-entity-editor :as ee]))

(defn schema-view-old []
  [firebase/on ["schema"]
   (fn [schema r]
     [:div
      [common/entity-editor "Schema" @schema
       :a :b
       (fn add-attribute [v]
         (firebase/ref-push r {"name" v}))
       (fn remove-attribute [id]
         (.remove (.child r id)))
       (fn add-attribute-attribute [id k]
         (.set (.child (.child r id) k) "hello"))
       (fn remove-attribute-attribute [id k]
         (.remove (.child (.child r id) k)))]])])

(defn types-editor []
  [ee/db-entity-editor "type" "Types" '{:type/name _}])

(defn nodes-editor []
  [ee/db-entity-editor "node" "Nodes" '{:node/type _}])

(defn types-view []
  [:div
   [types-editor]
   [nodes-editor]])
