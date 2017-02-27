(ns algopop.leaderboardx.app.db-firebase
  (:require
    [algopop.leaderboardx.app.firebase :as firebase]
    [reagent.core :as reagent]
    [algopop.leaderboardx.app.firebase-serialization :as s]))

;; TODO: how to specify a dataflow that will destroy itself?

(defn edgess [edges in-edges out-edges]
  [:div
   (for [edge (concat @in-edges @out-edges)
         :let [edge-name (get edge "name")]
         :when edge-name]
     ^{:key edge-name}
     [firebase/subaeon
      edges
      ["entities" edge-name]])])

(defn nodess [nodes edges]
  (reagent/with-let [out-edges (reagent/atom nil)
                     in-edges (reagent/atom nil)]
    [:div
     (for [[k node] @nodes
           :let [node-name (get node "name")]
           :when node-name]
       ^{:key node-name}
       [:div
        [firebase/aeon
         out-edges
         ["entities"]
         (fn [r]
           (-> r
               (.orderByChild "from")
               (.equalTo k)))]
        [firebase/aeon
         in-edges
         ["entities"]
         (fn [r]
           (-> r
               (.orderByChild "to")
               (.equalTo k)))]])
     [edgess edges in-edges out-edges]]))

(defn mmm [nodes member-ofs]
  [:div
   [:h3 (pr-str @member-ofs)]
   (for [[k member-of] @member-ofs
         :let [node-name (get member-of "from")]
         :when node-name]
     ^{:key node-name}
     [firebase/subaeon
      nodes
      ["entities" node-name]])])

(defn replace-edges [source outs ins type]
  (when (seq source)
    (firebase/db-set ["entities" source] #js {:name source
                                              :created firebase/timestamp})
    (firebase/db-set ["entities" (str source "member")]
                     #js {:name (str source "member")
                          :from source
                          :to "beach-ball"})))

(defn destroy-rquery [t]
  (.off (:ref t))
  (if-let [children (:children t)]
    (doseq [child children]
      (destroy-rquery @child))))

;; fql

(defn rquery [a id v query & more-queries]
  (let [r (query (firebase/db-ref ["entities"]) id v)
        tree (atom {:ref r
                    :children {}})]
    (prn "ZOMG" id v)
    (doto r
      (.on "child_added"
           (fn [snapshot]
             (let [v (s/firebase->clj (.val snapshot))
                   k (s/firebase->clj (.-key snapshot))]
               (prn "GOT" v)
               (when (seq more-queries)
                 (swap! tree assoc-in [:children k] (apply rquery a k v more-queries)))
               (swap! a merge {id v}))))
      (.on "child_changed"
           (fn [snapshot]
             (swap! a merge (s/firebase->clj (.val snapshot)))))
      (.on "child_removed"
           (fn [snapshot]
             (let [v (s/firebase->clj (.val snapshot))
                   k (s/firebase->clj (.-key snapshot))
                   children (:children @tree)]
               (destroy-rquery @(get children k))
               (swap! a apply dissoc k)))))
    tree))

(defn watch-graph [id nodes edges]
  (reagent/with-let
    [q (rquery
         nodes
         id
         nil
         (fn [r id v]
           (-> r
               (.orderByChild "to")
               (.equalTo id)))
         (fn [r id v]
           (prn "edges" id v)
           (-> r
               (.orderByChild "from")
               (.equalTo (get v "from"))))
         (fn [r id v]
           (prn "zoot" id v (get v "from"))
           (-> r
               ;; TODO: type also
               ;; TODO: child isn't quite right... how to terminate?
               (.child (get v "from")))))]
    (prn "watching!!!" id)
    [:div
     [:h1 (pr-str @nodes)]
     [:h2 (pr-str @edges)]]
    (finally
      (destroy-rquery q))))

(defn replace-edges2 [source outs ins type]
  (when (seq source)
    (let [s (s/clj->firebase source)]
      (firebase/db-set ["entities" s] #js {:name (s/clj->firebase s)
                                                :created firebase/timestamp})
      (firebase/db-set ["entities" (str s "member")]
                       #js {:name (str s "member")
                            :from s
                            :to "beach-ball"}))))
