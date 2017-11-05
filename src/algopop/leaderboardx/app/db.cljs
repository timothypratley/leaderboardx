(ns algopop.leaderboardx.app.db
  (:require
    [algopop.leaderboardx.app.pagerank :as pagerank]
    [datascript.core :as d]
    [devcards.core :as dc :refer-macros [defcard deftest]]
    [reagent.ratom :as ratom :refer-macros [reaction]]
    [datascript.db :as db]))

(defonce schema
  {:assessment-type/name {:db/index true}
   :assessment/assessor {:db/valueType :db.type/ref}
   :assessment/date {}
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
   :assessee/name {:db/index true}
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

#_(defonce conn
  (doto
    (d/create-conn schema)
    (posh!)))

#_(defn add-assessment [coach player attrs]
  (transact!
    conn
    [{:coach coach
      :player player
      :attrs attrs}]))

#_(defonce seed
  (do
    ;; TODO: just make one map?
    (transact!
      conn
      [{:name "William"
        :somedata "something about William"}
       {:node/types #{{:node/type "person"
                       :node/color "white"
                       :node/charge -30}
                      {:node/type "class"
                       :node/color "green"
                       :node/charge 100}}}
       {:edge/types #{{:edge/type "likes"
                       :edge/color "#9ecae1"}
                      {:edge/type "dislikes"
                       :edge/distance 300
                       :edge/dasharray "5,5"
                       :edge/color "#9e0000"}}}])
    (add-assessment "Coach" "William" {:producivity 7})))

(def q-player
  '[:find ?s ?attrs (pull ?e [*])
    :where [?a :coach "Coach"]
    [?a :player "William"]
    [?a :attrs ?attrs]
    [?e :name "William"]
    [?e :somedata ?s]])

#_(defn player []
  (q q-player conn))

(def q-ac
  '[:find (pull ?e [*])
    :in $ ?template
    :where
    [$ ?e :assessment-template/name ?template]])

#_(defn assessment-components [name]
  (q q-ac conn name))

(def q-ac2
  '[:find (pull ?e [*])
    :in $ ?template
    :where
    [$ ?e :dom/value ?template]
    [$ ?e :dom/tag "template"]])

#_(defn ac2 [template]
  (q q-ac2 conn template))

#_(defcard ac2
  @(ac2 "player-assessment"))

(defn assess [ol]
  (for [i (range (count ol))]
    {:order i
     :line (ol i)}))

(defn rank-entities [ranks]
  (for [[id pagerank rank] ranks]
    {:db/id id
     :rank rank
     :pagerank pagerank}))

(def node-q
  '[:find [?e ...]
    :in $ ?name
    :where [?e :node/name ?name]])

#_(defn get-node-by-name
  ([node-name]
   (first (d/q node-q @conn node-name)))
  ([node-name default]
   (or (get-node-by-name node-name) default)))

#_(defn p [id]
  (posh/pull conn '[*] id))

(def nodes-q
  '[:find [?e ...]
    :where
    [?e :node/name ?name]])

(def edges-q
  '[:find [?e ...]
    :where
    [?e :from ?from]
    [?e :to ?to]])

#_(defn set-ranks! []
  (let [node-ids (d/q nodes-q @conn)
        es (d/pull-many @conn '[*] (d/q edges-q @conn))]
    (transact!
      conn
      (rank-entities (pagerank/ranks node-ids es)))))

#_(defn add-node [name]
  (transact!
    conn
    [{:node/name name}])
  (set-ranks!))

#_(defn pull-q
  ([conn query]
   (pull-q conn '[*] query))
  ([conn pattern query & args]
   (reaction
     (doall
       (for [e @(apply q query conn args)]
         @(posh/pull conn pattern e))))))

#_(defn watch-nodes []
  (pull-q conn nodes-q))

#_(defn watch-edges []
  (pull-q conn edges-q))

#_(defn update-nodes [nodes]
  (transact! conn nodes))

#_(defn get-out-edges [id]
  @(q '[:find ?edge
        :in $ ?node
        :where [?edge :from ?node]]
      conn
      id))

#_(defn get-in-edges [id]
  @(q '[:find ?edge
        :in $ ?node
        :where [?edge :to ?node]]
      conn
      id))

(defn merge-node [id node existing]
  (let [outs (get-out-edges id)
        ins (get-in-edges id)]
    (update-nodes
      (concat
        [[:db.fn/retractEntity id]
         (assoc node :db/id existing)]
        (for [[out] outs]
          {:db/id out
           :from existing})
        (for [[in] ins]
          {:db/id in
           :to existing})))))

(defn name-node [id new-name]
  (let [{:keys [node/name] :as node} (get-node-by-id id)]
    (when (not= new-name name)
      (if-let [existing (get-node-by-name new-name)]
        (merge-node id node existing)
        (add-node id new-name)))))

#_(defn edge-tx [edge-id from to]
  {:db/id edge-id
   :edge/name (str (d/pull @conn [:name/name] from) " to " (d/pull @conn [:name/name] to))
   :from from
   :to to})

#_(defn add-edge [from to]
  (transact! conn [(edge-tx -1 from to)])
  (set-ranks!))

(defn replace-edges-entities [node-name outs ins edge-type]
  (let [node-id (get-node-by-name node-name -1)
        out-count (count outs)
        in-count (count ins)
        outs-start -2
        out-ids (map get-node-by-name outs (iterate dec outs-start))
        ins-start (- outs-start out-count)
        in-ids (map get-node-by-name ins (iterate dec ins-start))
        out-edges-start (- ins-start in-count)
        out-edge-ids (iterate dec out-edges-start)
        in-edges-start (- out-edges-start out-count)
        in-edge-ids (iterate dec in-edges-start)]
    ;; TODO: is this just concat?
    (->
      ;; node entity
      [{:db/id node-id
        :node/name node-name}]
      ;; related out and in target node entities
      (into
        (for [[id adjacent-node-name] (map vector (concat out-ids in-ids) (concat outs ins))]
          {:db/id id
           :node/name adjacent-node-name}))
      ;; out edge entities
      (into
        (for [[out-id edge-id adjacent-node-name] (map vector out-ids out-edge-ids outs)]
          {:db/id edge-id
           :edge/name (str node-name " to " adjacent-node-name)
           :edge/type edge-type
           :from node-id
           :to out-id}))
      ;; in edge entities
      (into
        (for [[in-id edge-id adjacent-node-name] (map vector in-ids in-edge-ids ins)]
          {:db/id edge-id
           :edge/name (str adjacent-node-name " to " node-name)
           :edge/type edge-type
           :from in-id
           :to node-id})))))

#_(defn replace-edges [k outs ins edge-type]
  (transact! conn (replace-edges-entities k outs ins edge-type))
  (set-ranks!))

#_(defn replace-many-edges [xs edge-type]
  (doseq [[k outs ins] xs]
    (transact! conn (replace-edges-entities k outs ins edge-type)))
  (set-ranks!))

(defn nodes-for-table []
  (reaction (sort-by :rank @(watch-nodes))))

(def outs-q
  '[:find [?name ...]
    :in $ ?node
    :where
    [?edge :from ?node]
    [?edge :to ?out]
    [?out :node/name ?name]])

#_(defn outs [id]
  (q outs-q conn id))

(def ins-q
  '[:find [?name ...]
    :in $ ?node
    :where
    [?edge :to ?node]
    [?edge :from ?in]
    [?in :node/name ?name]])

#_(defn ins [id]
  (q ins-q conn id))

(defn values [attribute]
  [:find '[(pull ?value [*]) ...]
   :where
   ['_ attribute '?value]])

(def node-types-q
  '[:find [?type ...]
    :where
    [?e :node/types ?type]
    [?type :node/type ?t]])

(def edge-types-q
  '[:find [?type ...]
    :where
    [?e :edge/types ?type]
    [?type :edge/type ?t]])

#_(defn node-types []
  (pull-q conn node-types-q))

#_(defn edge-types []
  (pull-q conn edge-types-q))

#_(defn insert! [e]
  (transact! conn [e]))

(defn add-node-type [v])
(defn add-edge-type [v]
  {:edge/types #{{:edge/type v
                  :edge/color "blue"
                  :edge/distance "30"}}})
(defn remove-node-type [])
(defn remove-edge-type [])
