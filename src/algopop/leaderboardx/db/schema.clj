(ns algopop.leaderboardx.db.schema
  (:require [algopop.leaderboardx.db :as db]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [datomic.api :as d]
            [datomic-schema.schema :as s]))

(def schema
  '{assessment
    {type [:ref :one]
     assessee [:ref :one]
     assessor [:ref :one]
     status [:enum [:pending :complete :cancelled]]
     date [:instant]
     duration-minutes [:long]}

    assessee
    {name [:string :indexed]
     tag [:ref :many]
     group [:ref :many]}

    assessment-template
    {name [:string]
     type [:string]
     idx [:long]
     child [:ref :many :isComponent]}

    user
    {email [:string :indexed]
     password [:string "Hashed password string"]
     status [:enum [:pending :active :inactive :cancelled]]}

    organization
    {name [:string :indexed]
     administrator [:ref :many]}

    tag
    {name [:string]}

    group
    {name [:string]
     organization [:ref :one]}})

(defn collect-field [acc [nm [tp & opts]]]
  (assoc acc (name nm) [tp (set opts)]))

(defn collect-fields
  [field-defs]
  {:fields (reduce collect-field {} field-defs)})

(defn expand-to-datomic [entity-defs]
  (s/generate-schema
   (for [[entity fields] entity-defs]
     (s/schema* (name entity) (collect-fields fields)))
   {:gen-all? false}))

(defn migrate []
  (db/transact (expand-to-datomic schema)))

(defn map-intersection
  [m1 m2]
  (into {}
        (for [[k v] m1
              :when (= v (m2 k))]
          [k v])))

(def datascript-attrs
  {:db/cardinality :db.cardinality/many
   :db/valueType :db.type/ref
   :db/index true})

(defn reshape [{:keys [db/ident] :as m}]
  [ident (map-intersection datascript-attrs m)])

(defn expand-to-datascript [entity-defs]
  (->> entity-defs
       (expand-to-datomic)
       (filter map?)
       (map reshape)
       (into {})))

(def datascript-schema
  (expand-to-datascript schema))

#_(db/transact [{:db/id #db/id[:db.part/user]
                :assessment-template/name "foo"
                :assessment-template/child [{:db/id #db/id[:db.part/user]
                                             :assessment-template/name "child"}]}])

#_(db/q '[:find (pull ?e [*])
        :where
        [?e :assessment-template/name "foo"]])
