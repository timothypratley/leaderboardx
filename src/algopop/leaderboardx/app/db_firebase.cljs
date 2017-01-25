(ns algopop.leaderboardx.app.db-firebase
  (:require
    [algopop.leaderboardx.app.firebase :as firebase]
    [reagent.core :as reagent]))

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

(defn watch-graph [nodes edges root]
  (reagent/with-let [member-ofs (reagent/atom nil)]
    (prn "watching!!!" root)
    [:div
     [:h1 (pr-str @nodes)]
     [:h2 (pr-str @edges)]
     ;;[:h3 (pr-str @member-ofs)]
     [firebase/aeon
      member-ofs
      ["entities"]
      (fn [r]
        (-> r
            ;; TODO: type also
            (.orderByChild "to")
            (.equalTo root)))]
     [mmm nodes member-ofs]
     [nodess nodes edges]]))

(defn replace-edges [source outs ins type]
  (when (seq source)
    (firebase/db-set ["entities" source] #js {:name source
                                              :created firebase/timestamp})
    (firebase/db-set ["entities" (str source "member")]
                     #js {:name (str source "member")
                          :from source
                          :to "beach-ball"})))
