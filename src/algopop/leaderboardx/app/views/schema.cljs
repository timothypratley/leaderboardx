(ns algopop.leaderboardx.app.views.schema
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.firebase :as firebase]))

(defn schema-view []
  [firebase/on ["schema"]
   (fn [schema r]
     [:div
      [common/entity-editor "Schema" @schema
       :a :b (atom nil)
      (fn add-attribute [v]
         (firebase/ref-push r {"name" v}))
       (fn remove-attribute [id]
         (.remove (.child r id)))
       (fn add-attribute-attribute [id k]
         (prn "hi" id k)
         (.set (.child (.child r id) k) "hello"))
       (fn remove-attribute-attribute [id k]
         (.remove (.child (.child r id) k)))]])])
