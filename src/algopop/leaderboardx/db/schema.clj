(ns algopop.leaderboardx.db.schema
  (:require
   [algopop.leaderboardx.db :as db]
   [datomic.api :as d]
   [datomic-schema.schema :as s]))

#_(defn riddiculous [x]
    (s/generate-schema
     (for [[e fields] entities]
       (s/schema e (s/fields ~@(for [f fields]))))))

(def schema
  (s/generate-schema
   [(s/schema
     assessment
     (s/fields
      [type :ref :one]
      [assessee :ref :one]
      [assessor :ref :one]
      [status :enum [:pending :complete :cancelled]]
      [date :instant]
      [duration-minutes :long]))
    (s/schema
     assessee
     (s/fields
      [name :string :indexed]
      [tag :ref :many]
      [group :ref :many]))
    (s/schema
     assessment-type
     (s/fields
      [name :string :indexed]
      [attribute :ref :many]))
    (s/schema
     user
     (s/fields
      [email :string :indexed]
      [password :string "Hashed password string"]
      [status :enum [:pending :active :inactive :cancelled]]))
    (s/schema
     organization
     (s/fields
      [name :string :indexed]
      [administrator :ref :many]))
    (s/schema
     tag
     (s/fields
      [name :string]))
    (s/schema
     group
     (s/fields
      [name :string]
      [organization :ref :one]))]))

(defn migrate [conn]
  (db/transact schema))
