(ns algopop.leaderboardx.db.ordered
  "http://dbs-are-fn.com/2013/datomic_ordering_of_cardinality_many/")

{:db/id #db/id[:db.part/user]
 :db/ident :append-position-in-scope
 :db/doc "Atomically adds to the end of a list of sorted cardinality/many lists"
 :db/fn #db/fn {:lang "clojure"
                :params [db scope-id scope-attr new-id pos-attr]
                :code [[:db/add new-id pos-attr
                        (->>
                         ;; The full set of children
                         (scope-attr (datomic.api/entity db scope-id))
                         ;; Get only the numerical position attr
                         (map pos-attr)
                         ;; Get the highest one
                         (reduce max 0)
                         ;; And increment by one
                         (inc))]]}}

(d/transact
  [[:db/add todolist-id :todolist/todoitems todoitem-tempid]
   [:db/add todoitem-tempid :todoitem/text "Remember the milk"]
   [:append-position-in-scope
    todolist-id
    :todolist/todoitems
    todoitem-tempid
    :todoitem/position]])



{:db/id #db/id[:db.part/user]
 :db/ident :reset-position-in-scope
 :db/doc "Goes through existing positions and sequentializes them, assuming
          retracted-eid is being retracted from the list"
 :db/fn #db/fn {:lang "clojure"
                :params [db scope-id scope-attr retracted-eid sorted-attr]
                :code (map-indexed
                       (fn [idx entity-id]
                         [:db/add entity-id sorted-attr idx])
                       (->>
                        ;; The full set of children
                        (scope-attr (datomic.api/entity db scope-id))
                        ;; Sort them by the numerical sorted-attr
                        (sort-by sorted-attr)
                        ;; Get the entity IDs
                        (map :db/id)
                        ;; Remove the retracted entity from the list
                        (filter (partial not= retracted-eid))))}}

(d/transact
  [[:db.fn/retractEntity todoitem-id]
   [:reset-position-in-scope
    todolist-id
    :todolist/todoitems
    todoitem-id
    :todoitem/position]])


{:db/id #db/id[:db.part/user]
 :db/ident :set-position-in-scope
 :db/doc "Reposition consistently"
 :db/fn #db/fn {:lang "clojure"
                :params [db scope-id scope-attr sorted-eids sorted-attr]
                :code (map-indexed
                       (fn [idx entity-id]
                         [:db/add entity-id sorted-attr idx])
                       (concat
                        sorted-eids
                        (clojure.set/difference
                         (map :db/id (scope-attr (datomic.api/entity db scope-id)))
                         sorted-eids)))}}

(d/transact
  [[:db.fn/retractEntity todoitem-id]
   [:set-position-in-scope
    todolist-id
    :todolist/todoitems
    [56 21 92 10]
    :todoitem/position]])
