(ns algopop.leaderboardx.app.db-firebase
  (:require
    [cljs.pprint :as pprint]
    [algopop.leaderboardx.app.firebase :as firebase]
    [reagent.core :as reagent]
    [algopop.leaderboardx.app.firebase-serialization :as s]))

(defn unlisten
  "Stops listening to a query tree."
  [a t]
  (.off (:ref @t))
  (when-let [children (:children @t)]
    (doseq [[k child] children]
      (swap! a dissoc k)
      (unlisten a child))))

(defn listen
  "The input atom a will be modified to contain entities found by applying queries.
  Successive queries are applied to the results of the previous query,
  creating a tree of firebase references.
  Queries are functions that return a reference https://firebase.google.com/docs/reference/js/firebase.database.Query.
  All entities are unique and live in firebase under the entities path.
  The reference tree is returned.
  To stop listening to updates, call unlisten on the reference tree."
  [a parent-k parent-v q & qs]
  (let [r (q (firebase/user-entities) parent-k parent-v)
        query-node (atom {:ref r
                          :children {}})]
    (doto r
      (.on "child_added"
           (fn child-added [snapshot]
             (let [k (s/firebase->clj (.-key snapshot))
                   v (s/firebase->clj (.val snapshot))]
               (when (seq qs)
                 (swap! query-node assoc-in [:children k]
                        (apply listen a k v qs)))
               (swap! a assoc k v))))
      (.on "child_changed"
           (fn child-changed [snapshot]
             (swap! a update (s/firebase->clj (.-key snapshot))
                    merge (s/firebase->clj (.val snapshot)))))
      (.on "child_removed"
           (fn child-removed [snapshot]
             (let [k (s/firebase->clj (.-key snapshot))
                   children (:children @query-node)
                   child (get children k)]
               (swap! a dissoc k)
               (when child
                 (unlisten a child))))))
    query-node))

(defn watch-entities [parent-k a]
  (reagent/with-let
    [reference-tree
     (listen
       a
       parent-k
       nil
       (fn get-edges-to-the-root [r k v]
         (-> r
             (.orderByChild "to")
             (.equalTo k)))
       (fn get-all-edges-from-nodes-connected-to-the-root [r k v]
         (-> r
             (.orderByChild "from")
             (.equalTo (get v "from"))))
       ;; TODO: not quite right (pollutes top level because child returned without key)
       ;; Need it for information about the nodes
       #_(fn get-the-from-nodes [r k v])
         (-> r
            (.orderByKey
             (.equalTo (get v "from")))))]
    (finally
      (unlisten a reference-tree))))

(defn watch-graph2 []
  (firebase/db-ref []))


(defn membership [obj graph-name from to edge-name]
  (doto obj
    (aset (s/clj->firebase (str edge-name "-member-of-" graph-name))
          #js {:from edge-name
               :to graph-name
               :edge-type "member-of"})
    (aset (s/clj->firebase (str from "-member-of-" graph-name))
          #js {:from from
               :to graph-name
               :edge-type "member-of"})
    (aset (s/clj->firebase (str to "-member-of-" graph-name))
          #js {:from to
               :to graph-name
               :edge-type "member-of"})))

;; TODO: created vs modified
(defn with-edge [obj graph-name from to node-type edge-type]
  (let [edge-name (str from "-" edge-type "-" to)]
    (doto obj
      (aset (s/clj->firebase from)
            #js {:created firebase/timestamp
                 :node-type node-type})
      (aset (s/clj->firebase to)
            #js {:created firebase/timestamp
                 :node-type node-type})
      (aset (s/clj->firebase edge-name)
            #js {:from from
                 :to to
                 :edge-type edge-type})
      (membership graph-name from to edge-name))))

(defn build-update [obj graph-name entity-name node-type edge-type [out & more-outs :as outs] [in & more-ins :as ins]]
  (cond
    in (recur
         (with-edge obj graph-name in entity-name node-type edge-type)
         graph-name
         entity-name
         node-type
         edge-type
         outs
         more-ins)
    out (recur
          (with-edge obj graph-name entity-name out node-type edge-type)
          graph-name
          entity-name
          node-type
          edge-type
          more-outs
          ins)
    :else obj))

(defn replace-edges [graph-name entity-name node-type edge-type outs ins]
  ;; TODO: delete old edges!
  (when (seq entity-name)
    (let [entity-name (s/clj->firebase entity-name)]
      (firebase/ref-update
        [(firebase/user-entities)]
        (build-update
          (clj->js {(s/clj->firebase entity-name) {:created firebase/timestamp
                                                   :node-type node-type}})
          graph-name
          entity-name
          node-type
          edge-type
          outs
          ins)))))
