(ns algopop.leaderboardx.app.pagerank)

(defn normalize-vector
  "Returns a proportional vector of sum 1"
  [v]
  (let [sum (reduce + v)]
    (if (zero? sum)
      v
      (for [x v]
        (/ x sum)))))

(defn transpose [g]
  (apply map vector g))

(defn normalize-matrix
  "Returns a normalized matrix such that every column sums to 1"
  [g]
  (transpose (map normalize-vector (transpose g))))

(defn diff [a b]
  (if (< a b) (- b a) (- a b)))

(defn vector-diff [a b]
  (reduce + (map diff a b)))

(defn vector-almost-equal? [a b epsilon]
  (< (vector-diff a b) epsilon))

(defn pagerank-row
  "multiply inlinks and their pageranks"
  [prs row]
  (reduce + (map * prs row)))

(defn stabilized? [x-old x epsilon]
  (vector-almost-equal? x-old x epsilon))

(defn normalize-pageranks
  "Account for new session starts."
  [prs]
  (let [n (count prs)
        sum (reduce + prs)
        d (/ (- 1 sum) n)]
    (for [pr prs]
      (+ pr d))))

(defn improve
  "Given existing pageranks, calculate new pageranks."
  [prs g click-through]
  (normalize-pageranks
   (for [row g]
     (* click-through (pagerank-row prs row)))))

(defn iteratively-improve
  "Calls the improve function until the difference between iterations is less than epsilon."
  [g click-through epsilon previous prs]
  (if (stabilized? previous prs epsilon)
    prs
    (recur g click-through epsilon prs (improve prs g click-through))))

(defn pagerank
  "Calculates the pagerank of each node in a graph."
  ([g]
   (pagerank g 0.85 0.001))
  ([g click-through epsilon]
   (let [n (count g)
         previous (repeat n 1)
         initial-pageranks (repeat n (/ 1 n))]
     (iteratively-improve (normalize-matrix g) click-through epsilon previous initial-pageranks))))
