(ns algopop.leaderboardx.app.db
  (:require [algopop.leaderboardx.app.views.common :as common]
            [cljs.pprint :as pprint]
            [clojure.walk :as walk]
            [datascript :as d]
            [reagent.ratom :as ratom :include-macros]))

(defonce schema
  {:friend {:db/valueType :db.type/ref
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
  (common/bind conn q-player))

(def q-ac
  `[:in $ ?template
    :find ?c
    :where
    [$ ?e :assessment-template ?template]
    [$ ?e :components ?c]])

(defn assessment-components []
  (common/bind conn q-ac))

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

(def edges-q
  '[:find ?e (pull ?e [*])
    :where [?e :edge/name ?name]])

(defn get-edges []
  (into {} (d/q edges-q @conn)))

(defn watch-nodes []
  (common/bind conn nodes-q))

(defn watch-edges []
  (common/bind conn edges-q))

(defn replace-edges2 []
  (d/transact!
   conn
   [[:db/add -1 :foo/name "foo"]
    [:db/add -2 :bar/name "bar"]
    [:db/add -1 :friend -2]
    [:db/add -2 :friend -1]]))

(defn replace-edges [k outs ins]
  (d/transact!
   conn
   (->
    [{:db/id -1
      :node/name k}]
    (into
     (for [[oi o] (map vector (map - (drop 2 (range))) outs)]
       {:db/id oi
        :node/name o}))
    (into
     (for [[ei o] (map vector (map - (drop 1000 (range))) outs)]
       {:db/id ei
        :edge/name (str k " to " o)}))
    (into
     (for [[x y o] (map vector (map - (drop 2 (range))) (map - (drop 1000 (range))) outs)]
       {:db/id y
        :from -1
        :to x}))))
  {:nodes (get-nodes)
   :edges (get-edges)})

(defn f [acc [from to x]]
  (assoc-in acc [from to] x))

(defn get-graph []
  (let [ns (watch-nodes)
        es (watch-edges)]
    (ratom/reaction
     {:nodes (into {} @ns)
      :edges (reduce f {} (for [[id {:keys [from to] :as e}] @es]
                            [(:db/id (first from)) (:db/id (first to)) e]))})))

(defn d3-graph [nodes edges]
    (let [d3nodes (concat nodes edges)
          name->idx (zipmap (map :id d3nodes) (range))
          d3nodes (map walk/stringify-keys d3nodes)]
      (clj->js
       {:nodes d3nodes
        :idx name->idx
        :paths (for [[source targets] edges
                     [target] targets]
                 [(name->idx source)
                  (name->idx [source target])
                  (name->idx target)])
        :links (apply
                concat
                (for [[source targets] edges
                      [target {:keys [db/id]}] targets]
                  [{:link [source id]
                    :source (name->idx source)
                    :target (name->idx id)}
                   {:link [id target]
                    :source (name->idx id)
                    :target (name->idx target)}]))})))
