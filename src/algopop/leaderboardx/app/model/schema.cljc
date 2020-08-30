(ns algopop.leaderboardx.app.model.schema)

; Skeleton ontology, extendable
;; an attribute inheritance tree: :schema/extends type system
;;; do user schema change affect database schema???

;; flexible schema concrete data
;; nodes are entities
;; edges are relations with attributes, nodes with a from/to
;; sets can be node+edge-type, unions or *queries*
;; types affect UI fields displayed
;; attributes are values or relations, value are an artificial terminator
;; events have ordinal time

; Node
;; Person
;;;; Student, Staff
;; Document
;; Event
;;; Evaluation
;; Concept
; Set
;; Group
;; Team
;; Tag
;; Type

; Edge
;; Relation
;;;; Chain of command, reporting,
;; Reference
;; Membership
;; Subject
;; Owner
;; Requester

;;; do types even help? is inheritance a thing?

;; relations + events? or always indirect edges? (later)

#_(def schema
  (merge
   #:type{:name #:db{:unique :db.unique/identity}
          :extends #:db{:valueType :db.type/ref
                        :cardinality :db.cardinality/many}
          :connects-to #:db{:valueType :db.type/ref}
          :connects-from #:db{:valueType :db.type/ref}
          :cardinality {}}

   #:node{:type #:db{:valueType :db.type/ref}}

   #:edge{:type #:db{:valueType :db.type/ref}
          :from #:db{:valueType :db.type/ref}
          :to #:db{:valueType :db.type/ref}}

   ;; TODO: this comes from user data; modify schema on the fly
   ;; the implication is user can modify schema safely and are subject to the same restrictions
   #:person{:name #:db{:unique :db.unique/identity}}
   #:role{:name #:db{:unique :db.unique/identity}}
   #:duty{:name #:db{:unique :db.unique/identity}}

   ))

#_(defonce schema-old
    {:assessment-type/name {:db/index true}
     :assessment/assessor {:db/valueType :db.type/ref}
     :assessment/date {}

     :assessee/name {:db/index true}
     :assessee/group {:db/cardinality :db.cardinality/many
                      :db/valueType :db.type/ref}

     :edge/types {:db/cardinality :db.cardinality/many
                  :db/valueType :db.type/ref
                  :db/isComponent true}
     :node/types {:db/cardinality :db.cardinality/many
                  :db/valueType :db.type/ref
                  :db/isComponent true}

     :edge/type {}
     :edge/name {}

     :node/type {}
     :node/name {}

     :group/name {}

     :user/password {}

     :assessment/type {:db/valueType :db.type/ref}
     :user/status {:db/valueType :db.type/ref}
     :group/organization {:db/valueType :db.type/ref}
     :organization/administrator {:db/cardinality :db.cardinality/many
                                  :db/valueType :db.type/ref}
     :assessment/status {:db/valueType :db.type/ref}
     :assessee/tag {:db/cardinality :db.cardinality/many
                    :db/valueType :db.type/ref}
     :user/email {:db/index true}
     :tag/name {}
     :assessment/duration-minutes {}
     :assessment/assessee {:db/valueType :db.type/ref}
     :assessment-type/attribute {:db/cardinality :db.cardinality/many
                                 :db/valueType :db.type/ref}
     :organization/name {:db/index true}

     ;; stuff
     :dom/child {:db/cardinality :db.cardinality/many
                 :db/valueType :db.type/ref
                 :db/isComponent true}

     :from {:db/valueType :db.type/ref}
     :to {:db/valueType :db.type/ref}})

#_(def schema-old-old
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
