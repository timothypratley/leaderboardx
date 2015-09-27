(ns algopop.leaderboardx.app.db
  (:require [cljs-uuid.core :as uuid]
            [clojure.walk :as walk]
            [datascript :as d]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom :include-macros]))

;; TODO:
#_(defn bibind [conn query write]
  (let [k (uuid/make-random)
        r (ratom/make-reaction
           (fn [])
           :auto-run true
           :on-set (fn [a b]
                     (println "HELLO! YOU SET ME!")
                     (d/transact! conn (write b))
                     b))]
    (d/listen! conn k
               (fn [tx-report]
                 (reset! r (d/q q (:db-after tx-report)))))
    (set! (.-__key r) k)
    r))

(defn bind
  ([conn q]
   (bind conn q (reagent/atom nil)))
  ([conn q state]
   (let [k (uuid/make-random)]
     (reset! state (d/q q @conn))
     (d/listen! conn k (fn [tx-report]
                         (reset! state (d/q q (:db-after tx-report)))))
     (set! (.-__key state) k)
     state)))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))

(defonce schema
  {;; TODO: don't need direct links like friend anymore?
   :friend {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :from {:db/valueType :db.type/ref
          :db/cardinality :db.cardinality/many}
   :to {:db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many}})
(defonce conn (d/create-conn schema))

(defn add-assessment [coach player attrs]
  (d/transact!
   conn
   [{:coach coach
     :player player
     :attrs attrs}]))

(defonce seed
  (do
    (d/transact!
     conn
     [{:name "William"
       :somedata "something about William" }])
    (add-assessment "Coach" "William" {:producivity 7})
    (d/transact!
     conn
     [{:assessment-template :player-assessment
       :components [[:metrics "Metrics" ["Productivity" "Leadership" "Happiness"]]
                    [:ol "Achievements"]
                    [:ol "Weaknesses"]
                    [:ol "Coach goals"]
                    [:ol "Player goals"]
                    [:textarea "Coach comments"]
                    [:textarea "Player comments"]]}])))

(def q-player
  '[:find ?s ?attrs (pull ?e [*])
    :where [?a :coach "Coach"]
    [?a :player "William"]
    [?a :attrs ?attrs]
    [?e :name "William"]
    [?e :somedata ?s]])

(defn player []
  (bind conn q-player))

(def q-ac
  `[:in $ ?template
    :find ?c
    :where
    [$ ?e :assessment-template ?template]
    [$ ?e :components ?c]])

(defn assessment-components []
  (bind conn q-ac))

;;(println (d/q q-ac @conn :player-assessment))

(defn insert []
  (d/transact!
   conn
   [{:db/id 3
     :achievements #{"Won the spelling bee."}
     :components [[:metrics "Metrics" ["Productivity"
                                       "Leadership"
                                       "Happiness"]]
                  [:ol "Achievements" ["Won the spelling bee."]]
                  [:ol "Weaknesses"]
                  [:ol "Coach goals"]
                  [:ol "Player goals"]
                  [:textarea "Coach comments"]
                  [:textarea "Player comments"]]}]))

(defn assess [ol]
  (for [i (range (count ol))]
    {:order i
     :line (ol i)}))

(defn rank-entities [ranks]
  (for [[id rank pagerank] ranks]
    {:db/id id
     :rank rank
     :pagerank pagerank}))

;; TODO: how to combine this with a single transaction?
;; use with-db??
(defn set-ranks [ranks]
  (d/transact! conn (rank-entities ranks)))

(defn add-edge [name]
  (d/transact!
   conn
   [{:edge/name name}]))

(defn add-node [name]
  (println "ADDING" name)
  (d/transact!
   conn
   [{:node/name name}]))

(def nodes-q
  '[:find ?e (pull ?e [*])
    :where [?e :node/name ?name]])

(defn get-nodes []
  (into {} (d/q nodes-q @conn)))

(def node-q
  '[:find ?e
    :in $ ?name
    :where [?e :node/name ?name]])

(defn get-node
  ([name]
   (ffirst (d/q node-q @conn name)))
  ([name default]
   (or (get-node name) default)))

(defn pull [id]
  (d/pull @conn '[*] id))

(def edges-q
  '[:find ?e (pull ?e [*])
    :where [?e :edge/name ?name]])

(defn get-edges []
  (into {} (d/q edges-q @conn)))

(defn watch-nodes []
  (bind conn nodes-q))

(defn watch-edges []
  (bind conn edges-q))

(defn update-nodes [nodes]
  (d/transact! conn nodes))

(defn get-out-edges [id]
  (d/q '[:find ?edge
         :in $ ?node
         :where [?edge :from ?node]]
       @conn
       id))

(defn get-in-edges [id]
  (d/q '[:find ?edge
         :in $ ?node
         :where [?edge :to ?node]]
       @conn
       id))

(defn rename-node [id new-name]
  (let [{:keys [node/name] :as node} (pull id)]
    (when (not= new-name name)
      (if-let [existing (get-node new-name)]
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
               :to existing}))))
        (update-nodes
         [{:db/id id
           :node/name new-name}])))))

;; TODO: if node already exists ^^
(defn replace-edges-entities [k outs ins]
  (let [out-count (count outs)
        in-count (count ins)
        outs-start -2
        out-ids (map get-node outs (iterate dec outs-start))
        ins-start (- outs-start out-count)
        in-ids (map get-node ins (iterate dec ins-start))
        out-edges-start (- ins-start in-count)
        out-edge-ids (iterate dec out-edges-start)
        in-edges-start (- out-edges-start out-count)
        in-edge-ids (iterate dec in-edges-start)]
    (->
     ;; node entity
     [{:db/id (get-node k -1)
       :node/name k}]
     ;; related out and in target node entities
     (into
      (for [[id name] (map vector (concat out-ids in-ids) (concat outs ins))]
        {:db/id id
         :node/name name}))
     ;; out edge entities
     (into
      (for [[out-id edge-id name] (map vector out-ids out-edge-ids outs)]
        {:db/id edge-id
         :edge/name (str k " to " name)
         :from -1
         :to out-id}))
     ;; in edge entities
     (into
      (for [[in-id edge-id name] (map vector in-ids in-edge-ids ins)]
        {:db/id edge-id
         :edge/name (str name " to " k)
         :from in-id
         :to -1})))))

(defn replace-edges [k outs ins]
  (d/transact! conn (replace-edges-entities k outs ins)))

(def table-nodes-q
  '[:find ?node ?rank ?name
    :in $
    :where
    [?node :node/name ?name]
    [(get-else $ ?node :rank nil) ?rank]])

(defn nodes-for-table []
  (let [nodes (bind conn table-nodes-q)]
    (ratom/reaction (sort-by second @nodes))))

(def outs-q
  '[:find [?name ...]
    :in $ ?node
    :where
    [?edge :from ?node]
    [?edge :to ?out]
    [?out :node/name ?name]])

(defn outs [id]
  (d/q outs-q @conn id))

(def ins-q
  '[:find [?name ...]
    :in $ ?node
    :where
    [?edge :to ?node]
    [?edge :from ?in]
    [?in :node/name ?name]])

(defn ins [id]
  (d/q ins-q @conn id))

;;; TODO: depricate
(defn f [acc [from to x]]
  (assoc-in acc [from to] x))

(defn get-graph []
  (let [ns (watch-nodes)
        es (watch-edges)]
    (ratom/reaction
     {:nodes (into {} @ns)
      :edges (reduce f {} (for [[id {:keys [from to] :as e}] @es]
                            [(:db/id (first from)) (:db/id (first to)) e]))})))
