(ns algopop.leaderboardx.roster.table-view
  (:require [algopop.leaderboardx.app.model.reactive-query :as rq]
            [algopop.leaderboardx.app.model.db :as db]
            [clojure.set :as set]
            [clojure.string :as str]
            [reagent.core :as reagent]
            [justice.core :as j]
            [taoensso.encore :as encore]
            [datascript.core :as d]
            [meander.strategy.epsilon :as s]))

(def days ["mon" "tues" "wed" "thurs" "fri"])

(defn duty-sort-key [{:duty/keys [name start]}]
  [start (str/lower-case name)])

(defn clj->json [x]
  (js/JSON.stringify (clj->js x)))

(defn json->clj [^string s]
  (js->clj (js/JSON.parse s)))

(defn drag-event->clj [^js/DragEvent e]
  (some-> (.getData (.-dataTransfer e) "application/json")
          (json->clj)))

(defn clj->drag-event [data ^js/DragEvent e]
  (.setData (.-dataTransfer e) "application/json" (clj->json data)))

(defn assignment-key [person-name day duty-name]
  (str person-name " -> " day ":" duty-name))

(defn assign [person-name day duty-name]
  (let [k (assignment-key person-name day duty-name)]
    #:edge{:name k
           :from [:person/name person-name]
           :to [:duty/name duty-name]
           :day day}))

(defn unassign [person-name day duty-name]
  [:db.fn/retractEntity [:edge/name (assignment-key person-name day duty-name)]])

(def drop-tx
  ;; TODO: does not remove redundant TX (already assigned)
  (s/match
   [["assignment" ?a-person ?a-day ?a-duty]
    ["assignment" ?b-person ?b-day ?b-duty]]
   (when (or (not= ?a-person ?b-person)
             (not= ?a-day ?b-day)
             (not= ?a-duty ?b-duty))
     [(unassign ?a-person ?a-day ?a-duty)
      (assign ?a-person ?b-day ?b-duty)
      (unassign ?b-person ?b-day ?b-duty)
      (assign ?b-person ?a-day ?a-duty)])

   [["assignment" ?a-person ?a-day ?a-duty]
    ["target" ?b-day ?b-duty]]
   (when (or (not= ?a-day ?b-day)
             (not= ?a-duty ?b-duty))
     [(unassign ?a-person ?a-day ?a-duty)
      (assign ?a-person ?b-day ?b-duty)])

   [["assignment" ?a-person ?a-day ?a-duty]
    ["person" ?b]]
   [(unassign ?a-person ?a-day ?a-duty)]

   [["person" ?a-person]
    ["assignment" ?b-person ?b-day ?b-duty]]
   (when (not= ?a-person ?b-person)
     [(unassign ?b-person ?b-day ?b-duty)
      (assign ?a-person ?b-day ?b-duty)])

   [["person" ?a-person]
    ["target" ?b-day ?b-duty]]
   [(assign ?a-person ?b-day ?b-duty)]


   ?else
   (prn "FAIL" ?else)))

(defn match-drop [dragging me]
  (some-> (drop-tx [dragging me])
          ;;(remove nil?)
          (j/transacte)))

(defn drop-target [handle-dropped me component]
  (let [targeted? (reagent/atom false)]
    (fn [attrs & children]
      (into
       [component
        (encore/nested-merge
         (cond->
          {:on-drag-over (fn allow-drop [^js/DragEvent e]
                           (.preventDefault e))
           :on-drop (fn [^js/DragEvent e]
                      (let [dragging (drag-event->clj e)]
                        (handle-dropped dragging me)))
           :on-drag-enter (fn [e]
                            (reset! targeted? true))
           :on-drag-leave (fn [e]
                            (reset! targeted? false))}
          @targeted? (assoc :style {:background-color "#E0E0FF"}))
         attrs)]
       children))))

(defn draggable [data component]
  (let [dragging? (reagent/atom false)]
    (fn [attrs & children]
      (into
       [component
        (encore/nested-merge
         (cond->
          {:draggable true
           :on-drag-start (fn [e]
                            (reset! dragging? true)
                            (clj->drag-event data e))
           :on-drag-end (fn [e]
                          (reset! dragging? false))}
          @dragging? (assoc :style {:background-color "#F0F0F0"}))
         attrs)]
       children))))

(defn card [attrs & children]
  (into
   [:div.panel
    (encore/nested-merge
     {:style {:background-color "#F4F4F8"
              :padding-left "0.33em"
              :margin-bottom "2px"}}
     attrs)]
   children))

(defn draggable-assignment [person-name assignment]
  (let [me ["assignment" person-name (:edge/day assignment) (:duty/name (:edge/to assignment))]
        ddcard (draggable me (drop-target match-drop me card))]
    [ddcard {}
     person-name
     [:button.close
      {:on-click (fn [e]
                   (db/retract-entity assignment))}
      "×"]]))

(defn assignment-target [day duty-name dragging?]
  (let [me ["target" day duty-name]
        dcard (drop-target match-drop me card)]
    [dcard {:class "unselectable"
            :style (cond-> {:background-color :swap/dissoc
                            :white-space "pre-wrap"}
                           dragging? (assoc :border "1px dotted lightgrey"))}
     " "]))

(defn draggable-person [person-name]
  (let [me ["person" person-name]
        ddcard (draggable me (drop-target match-drop me card))]
    [ddcard {} person-name]))

(defn roster []
  (let [dragging? (reagent/atom nil)
        [rduties dba] (rq/rq '#:duty{:name _})
        [rpeople dbb] (rq/rq '(:person/name _))
        [rassignments dbc] (rq/rq '#:edge{:from #:person{:name _}
                                          :to #:duty{:name _}})]
    (fn []
      ;; TODO: doesn't trigger on swap???
      @dba @dbb @dbc
      (let [all-person-names @rpeople
            duties (sort-by duty-sort-key @rduties)
            col-days (map-indexed vector days)
            row-duties (map-indexed vector duties)
            assignments (reduce
                         (fn [acc {:as assignment
                                   {duty-name :duty/name} :edge/to
                                   day :edge/day}]
                           (update-in acc [day duty-name] (fnil conj #{}) assignment))
                         {}
                         @rassignments)]
        [:div.well {:style {:display "grid"
                            :grid-template-columns "repeat(7, 1fr)"
                            :grid-gap "10px"
                            :background-color "white"}
                    :on-drag-start (fn [e]
                                     (reset! dragging? true))
                    :on-drag-end (fn [e]
                                   (reset! dragging? false))}
         [:div {:style {:grid-column 1
                        :grid-row 1
                        :border-bottom "solid black 1px"}}
          [:strong "Duty"]]
         [:div {:style {:grid-column 2
                        :grid-row 1
                        :border-bottom "solid black 1px"}}
          [:strong "Start"]]
         (for [[col day] col-days]
           [:div {:key col
                  :style {:grid-column (+ col 3)
                          :grid-row 1
                          :border-bottom "solid black 1px"}}
            [:strong day]])
         (for [[row {duty-name :duty/name}] row-duties]
           [:div {:key row
                  :style {:grid-column 1
                          :grid-row (+ row 2)}}
            duty-name])
         (for [[row {:duty/keys [start]}] row-duties]
           [:div {:key row
                  :style {:grid-column 2
                          :grid-row (+ row 2)}}
            start])
         (doall
          (for [[row {duty-name :duty/name}] row-duties
                [col day] col-days]
            [:div {:key [col row]
                   :style {:grid-column (+ col 3)
                           :grid-row (+ row 2)}}
             (doall
              (for [{{person-name :person/name} :edge/from
                     :as assignment} (sort-by (comp str/lower-case :person/name :edge/from)
                                              (get-in assignments [day duty-name]))]
                ^{:key person-name}
                [draggable-assignment person-name assignment]))
             [assignment-target day duty-name @dragging?]]))
         [:div {:style {:grid-column 1
                        :grid-row (+ (count duties) 3)}}
          "Available"]
         (doall
          (for [[col day] col-days
                :let [available
                      (sort-by str/lower-case
                       (set/difference
                        (set all-person-names)
                        (->> (get assignments day)
                             (vals)
                             (apply set/union)
                             (map :edge/from)
                             (map :person/name)
                             (set))))]]
            [:div {:key col
                   :style {:grid-column (+ col 3)
                           :grid-row (+ (count duties) 3)}}
             (doall
              (for [person-name available]
                ^{:key person-name}
                [draggable-person person-name]))]))]))))

(defn roster-summary [db]
  (let [{:keys [assignments duties days people]} @db
        counts (frequencies (vals assignments))
        cumulative (frequencies (for [[[day shift] name] assignments]
                                  [day name]))]
    [:div.well {:style {:background-color "white"}}
     [:table.table
      [:thead
       [:tr
        [:th "Person"]
        (for [day days]
          [:th {:key day} day])
        [:th "Assignments"]]]
      [:tbody
       (for [name (sort (keys people))]
         [:tr {:key name}
          [:td name]
          (doall
           (for [day days]
             [:td {:key day} (or (get cumulative [day name]) 0)]))
          [:td {:style {:text-align "right"}}
           (or (get counts name) 0)]])]]]))
