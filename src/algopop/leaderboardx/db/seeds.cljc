(ns algopop.leaderboardx.db.seeds)

(def assessment-template
  '[template player-assessment
    [[name assesse]
     [group metrics
      [[select5 productivity]
       [select5 leadership]
       [select5 happiness]]]
     [ol achievements]
     [ol weaknesses]
     [ol goach-goals]
     [ol player-goals]
     [textarea coach-comments]
     [textarea player-comments]]])

;; TODO: #+cljs (defn #db/id [[_ id]] id)
;; clj #db/id [:db.part/user id]

(defn expand
  ([v]
   (expand v 0 (atom 0)))
  ([[type title children] idx curr-id]
   (let [id (swap! curr-id dec)]
     (merge
      (when (= -1 id)
        {:db/id id})
      {:assessment-template/name (name title)
       :assessment-template/type (name type)
       :assessment-template/idx idx
       :assessment-template/child
       (for [[child-idx child] (map vector (range) children)]
         (expand child child-idx curr-id))}))))

(def tree
  (expand assessment-template))
