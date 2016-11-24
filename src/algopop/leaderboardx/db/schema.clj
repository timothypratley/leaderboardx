(ns algopop.leaderboardx.db.schema
  (:require [algopop.leaderboardx.system :as system]
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
     child [:ref :many :component]}

    user
    {email [:string :unique-identity]
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
  (println "Migrating schema...")
  (let [db (d/create-database system/datomic-uri)
        conn (d/connect system/datomic-uri)]
    (d/transact conn (expand-to-datomic schema))
    ;; TODO: does not exit (d/disconnect conn)
    (println "Schema transacted, Ctrl-C")))

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

(defn spit-datascript-schema []
  (spit "src-cljs/algopop/leaderboardx/app/schema.cljs"
        (str ";; This file is generated. Do not modify manually.\n"
             "(ns algopop.leaderboardx.app.schema)\n\n"
             (with-out-str
               (pprint/pprint (list 'def 'schema
                                 datascript-schema))))))
