(ns algopop.leaderboardx.roster.solve
  (:require [justice.core :as j]))

(defn infinite-shuffle [xs]
  (lazy-seq
   (concat
    (shuffle xs)
    (infinite-shuffle xs))))

(defn randomly-assign [duties days people]
  (apply concat
         (for [day days]
           (map
            (fn [{:keys [duty/name duty/start]} person]
              [name day start person])
            duties
            (infinite-shuffle people)))))

(defn valid? [duties people assignments]
  (and
   ;; limit of two recess/lunch/afternoon duties per week
   #_(every? true?
             (for [[person person-assignments] (group-by last assignments)]
               ;; TODO: how to nominate the duties?
               (frequencies (map first person-assignments))))

   ;; only one duty per day per person
   (every? true?
           (for [[day assignments-that-day] (group-by second assignments)]
             (apply distinct?
                    (map last assignments-that-day))))

   ;; TODO TODO
   ;; role of duty matches role of person
   #_(every?
      (fn [[duty-name day start-time person]]
        (if-let [roles (get-in duties [duty-name :roles])]
          (contains? roles (get-in people [person :node/role]))
          true))
      assignments)))

(defn solve-csp [{:keys [people duties days]}]
  (let [people-names (keys people)
        variable-keys (for [day days
                            [duty-name _] duties]
                        [day duty-name])
        variables (zipmap (map #(apply str %) variable-keys)
                          (repeat people-names))
        problem (clj->js {:timeStep 0
                          :variables variables
                          :constraints (for [day days
                                             [duty-name-i _] duties
                                             [duty-name-j _] duties
                                             :when (not= duty-name-i duty-name-j)]
                                         [(str day duty-name-i) (str day duty-name-j) not=])})
        solution (js/csp.solve problem)]
    (if (= solution "FAILURE")
      {}
      (zipmap
       (sort variable-keys)
       (vals (sort (js->clj solution)))))))

;; TODO: This is duplicated
(def days ["mon" "tues" "wed" "thurs" "fri"])

(defn solve-randomly []
  (let [people (j/q '(:person/name _)) ;; TODO: probably already got this if we care
        duties (j/q '{:duty/name _})
        solution (first
                  (filter
                   #(valid? duties people %)
                   (take 1000
                         (repeatedly
                          #(randomly-assign duties days people)))))]
    ;; arrange by assignment
    (into {}
          (for [[duty-name day time person] solution]
            [[day duty-name] #{person}]))))
