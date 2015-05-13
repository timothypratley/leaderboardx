(ns algopop.leaderboardx.app.seed)

(defn rand-char []
  (char (+ 97 (rand-int 26))))

(def vowel? #{\a \e \i \o \u})

(defn next-char [c]
  (let [nxt (rand-char)]
    (if (or (vowel? c) (vowel? nxt))
      nxt
      (recur c))))

(defn rand-name []
  (apply str (take (+ 3 (rand-int 3))
                   (iterate next-char (rand-char)))))

(def test-graph
  (let [ks (distinct (repeatedly 10 rand-name))
        nodes (into {} (for [k ks]
                         [k {:hair (rand-nth ["red" "brown" "black" "blonde"])}]))]
    {:nodes nodes
     :edges (into {} (for [k ks]
                       [k (into {} (for [x (remove #{k} (take (+ 1 (rand-int 2)) (shuffle ks)))]
                                     [x {:value 1}]))]))}))
