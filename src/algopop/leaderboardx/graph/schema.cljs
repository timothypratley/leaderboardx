(ns algopop.leaderboardx.graph.schema)

(def shapes
  ["circle"
   "ellipse"
   "triangle"
   "triangle-down"
   "square"
   "rectangle"])

(def shape-cycle
  "A map of shape->next"
  (zipmap shapes (rest (cycle shapes))))

(defn next-shape [shape]
  (get shape-cycle shape (second shapes)))
