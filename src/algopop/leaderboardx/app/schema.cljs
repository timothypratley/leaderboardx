;; This file is generated. Do not modify manually.
(ns algopop.leaderboardx.app.schema)

(def
 schema
 {:assessment-template/type {},
  :assessment-template/name {},
  :assessment/assessor {:db/valueType :db.type/ref},
  :assessment/date {},
  :assessee/group
  {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
  :group/name {},
  :user/password {},
  :assessee/name {:db/index true},
  :assessment/type {:db/valueType :db.type/ref},
  :user/status {:db/valueType :db.type/ref},
  :group/organization {:db/valueType :db.type/ref},
  :organization/administrator
  {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
  :assessment-template/child
  {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
  :assessment/status {:db/valueType :db.type/ref},
  :assessee/tag
  {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
  :assessment-template/idx {},
  :user/email {},
  :tag/name {},
  :assessment/duration-minutes {},
  :assessment/assessee {:db/valueType :db.type/ref},
  :organization/name {:db/index true}})
