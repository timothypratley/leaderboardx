(ns algopop.leaderboardx.app.views.roster
  (:require
    [algopop.leaderboardx.app.views.common :as common]
    [reagent.core :as reagent]
    [clojure.string :as string]
    [clojure.set :as set]))

;; TODO: would a vector of maps with types in them be better?
;; TODO: schema can define relations -> dropdown

(def db
  (reagent/atom
    {:assignments {}
     :duties {"Morning Bus" {:start-time "08:00"
                             :roles #{"Support"}}
              "Recess" {:start-time "11:00"}
              "Lunch" {:start-time "13:00"}
              "Library" {:start-time "13:00"}
              "Evening Bus" {:start-time "15:00"}}
     :days ["mon" "tues" "wed" "thurs" "fri"]
     :people {"Kate" {:role "Coordinator"}
              "Kim" {:role "Teacher"}
              "Kirstin" {:role "Teacher"}
              "Barry" {:role "Teacher"}
              "Brian" {:role "Support"}
              "Bob" {:role "Support"}}
     :roles {"Support" {}
             "Teacher" {}
             "Coordinator" {}}
     :rules {"Match duty role" {:rule [:in [:people :role] [:duties :roles]]}
             "Duties per day" {:rule [:<= [:match :people :duties :day-of-week] 1]}
             "Total duties" {:rule [:<= [:match :people :duties] 4]}}}))

(defn randomly-assign [duties days people]
  (apply concat
    (for [day days]
      (map
        (fn [[duty-name {:keys [start-time]}] person]
          [duty-name day start-time person])
        duties
        (take (count duties)
              (shuffle (keys people)))))))

(defn valid? [duties people assignments]
  (and
    ;; only one duty per day per person
    (every? true?
            (for [[day assignments-that-day] (group-by second assignments)]
              (apply distinct?
                     (map last assignments-that-day))))

    ;; role of duty matches role of person
    (every?
      (fn [[duty-name day start-time person]]
        (if-let [roles (get-in duties [duty-name :roles])]
          (contains? roles (get-in people [person :role]))
          true))
      assignments)))

(defn solve [{:keys [people duties days]}]
  (let [solution
        (first
          (filter
            #(valid? duties people %)
            (take 100000
                  (repeatedly
                    #(randomly-assign duties days people)))))]
    ;; arrange by assignment
    (into {}
          (for [[duty-name day time person] solution]
            [[day duty-name] person]))))

(defn solve! []
  (swap! db assoc :assignments (solve @db))
  (doseq [[person duties] (group-by val (:assignments @db))]
    (swap! db assoc-in [:people person :duties] (str (count duties)))))

(solve!)

(defn add-entity [t k]
  (swap! db assoc-in [t k] {}))

(defn remove-entity [t k]
  (swap! db update t dissoc k))

(defn add-attribute [t k ak av]
  (swap! db assoc-in [t k ak] av))

(defn remove-attribute [t k ak]
  (swap! db update-in [t k] dissoc ak))

(defn col-headings [days]
  (map-indexed vector days))

(defn row-labels [duties]
  (sort
    (distinct
      (map-indexed
        (fn [row [duty-name {:keys [start-time]}]]
          [row start-time duty-name])
        duties))))

(defn assignments-grid [db]
  (let [{:keys [assignments duties days]} @db]
    (for [[col day] (col-headings days)
          [row start-time duty-name] (row-labels duties)]
      [col row (get assignments [day duty-name])])))

(defn roster [db]
  (let [{:keys [assignments duties days people]} @db]
    (into
      [:div.well {:style {:display "grid"
                          :grid-template-columns "repeat(7, 1fr)"
                          :grid-gap "10px"
                          :background-color "white"}}
       [:div {:style {:grid-column 1
                      :grid-row 1
                      :border-bottom "solid black 1px"}}
        [:strong "Duty"]]
       [:div {:style {:grid-column 2
                      :grid-row 1
                      :border-bottom "solid black 1px"}}
        [:strong "Start"]]]
      (concat
        (for [[col day] (col-headings days)]
          [:div {:style {:grid-column (+ col 3)
                         :grid-row 1
                         :border-bottom "solid black 1px"}}
           [:strong day]])
        (for [[row start-time duty-name] (row-labels duties)]
          [:div {:style {:grid-column 1
                         :grid-row (+ row 2)}}
           duty-name])
        (for [[row start-time duty-name] (row-labels duties)]
          [:div {:style {:grid-column 2
                         :grid-row (+ row 2)}}
           start-time])
        (for [[col row person] (assignments-grid db)]
          [:div {:style {:grid-column (+ col 3)
                         :grid-row (+ row 2)}
                 :on-drag-start
                 (fn [e]
                   (prn "hi"))}
           person])

        [[:div {:style {:grid-column 1
                        :grid-row (+ (count duties) 3)}}
          "Available"]]
        (for [[col day] (col-headings days)]
          [:div {:style {:grid-column (+ col 3)
                         :grid-row (+ (count duties) 3)}}
           (string/join ", "
             (sort
               (set/difference
                 (set (keys people))
                 (set (map val
                           (filter (fn [[[d duty-name] person]]
                                     (= d day))
                                   assignments))))))])))))

(defn roster-page []
  (let [{:keys [people duties roles rules assignments]} @db]
    [:div
     [:button
      {:on-click
       (fn [e]
         (solve!))}
      "Randomize"]
     [roster db]
     [:div.well {:style {:background-color "white"}}
      [common/entity-editor
       "People"
       people
       #(add-entity :people %)
       #(remove-entity :people %)
       #(add-attribute :people %1 %2 %3)
       #(remove-attribute :people %1 %2)]]
     [:div.well {:style {:background-color "white"}}
      [common/entity-editor
       "Duties"
       duties
       #(add-entity :duties %)
       #(remove-entity :duties %)
       #(add-attribute :duties %1 %2 %3)
       #(remove-attribute :duties %1 %2)]]
     [:div.well {:style {:background-color "white"}}
      [common/entity-editor
       "Roles"
       roles
       #(add-entity :roles %)
       #(remove-entity :roles %)
       #(add-attribute :roles %1 %2 %3)
       #(remove-attribute :roles %1 %2)]]
     [:div.well {:style {:background-color "white"}}
      [common/entity-editor
       "Rules"
       rules
       #(add-entity :rules %)
       #(remove-entity :rules %)
       #(add-attribute :rules %1 %2 %3)
       #(remove-attribute :rules %1 %2)]]]))
