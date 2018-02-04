(ns algopop.leaderboardx.app.views.roster
  (:require
    [algopop.leaderboardx.app.views.common :as common]
    [reagent.core :as reagent]
    [clojure.string :as string]
    [clojure.set :as set]
    [algopop.leaderboardx.app.logging :as log]
    [cljs.tools.reader.edn :as edn]))

;; TODO: would a vector of maps with types in them be better?
;; TODO: schema can define relations -> dropdown

(defonce db
  (reagent/atom {}))

(defn set-simple-example! [db]
  (reset!
    db
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

(defn set-example! [db]
  (reset!
    db
    {:assignments {}
     :duties {"Morning Bus" {:start-time "08:15"
                             :duration 30
                             :roles #{"Support"}}
              "Morning" {:start-time "08:15"
                         :duration 30}
              "Recess Area 1" {:start-time "10:50"
                               :duration 30}
              "Recess Area 2" {:start-time "10:50"
                               :duration 30}
              "Recess Area 3" {:start-time "10:50"
                               :duration 30}
              "Recess Library" {:start-time "10:50"
                                :duration 30}
              "K-1 Eating Area" {:start-time "12:50"
                                 :duration 10}
              "2-5 Eating Area" {:start-time "12:50"
                                 :duration 10}
              "Lunch Area 1" {:start-time "13:00"
                              :duration 30}
              "Lunch Area 2" {:start-time "13:00"
                              :duration 30}
              "Lunch Area 3" {:start-time "13:00"
                              :duration 30}
              "Homework Club" {:start-time "13:00"
                               :duration 30}
              "Library" {:start-time "13:00"
                         :duration 30}
              "Fitzroy Extra" {:start-time "13:00"
                               :duration 30}
              "Curb Pickup" {:start-time "15:05"
                             :duration 35}
              "Fort Farwell" {:start-time "15:05"
                              :duration 35}
              "Bus Lines 2" {:start-time "15:05"
                             :duration 20}
              "Bus 2" {:start-time "15:20"
                       :duration 30}
              "Afternoon Homework Club" {:start-time "15:20"
                                         :duration 30}

              "Evening Bus" {:start-time "15:00"}}
     :days ["mon" "tues" "wed" "thurs" "fri"]
     :people {"Liz" {:role "Support"}
              "Kristy" {:role "Teacher"}
              "Jeremy" {:role "Teacher"}
              "Mandy" {:role "Coordinator"}
              "Amanda" {:role "Teacher"}
              "Paul" {:role "Support"}
              "Kelly" {:role "Coordinator"}
              "Janelle" {:role "Teacher"}
              "Irene" {:role "Support"}
              "Amy" {:role "Teacher"}
              "Bronwyn" {:role "Teacher"}
              "Gloria" {:role "Teacher"}
              "Jodie" {:role "Coordinator"}
              "Jodi" {:role "Teacher"}
              "Robyn" {:role "Teacher"}
              "Violeta" {:role "Teacher"}
              "Andrew" {:role "Coordinator"}
              "Alison" {:role "Teacher"}
              "Marina" {:role "Support"}
              "Svet" {:role "Support"}
              "Jon" {:role "Teacher"}
              "Aminata" {:role "Teacher"}
              "Rowena" {:role "Teacher"}
              "Sheree" {:role "Support"}
              "Terrence" {:role "Support"}}
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

    ;; role of duty matches role of person
    (every?
      (fn [[duty-name day start-time person]]
        (if-let [roles (get-in duties [duty-name :roles])]
          (contains? roles (get-in people [person :role]))
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
      (into {}
            (zipmap
              (sort variable-keys)
              (vals (sort (js->clj solution))))))))

(defn solve-randomly [{:keys [people duties days]}]
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

(defn solve! [solver]
  (swap! db assoc :assignments (solver @db))
  (doseq [[person duties] (group-by val (:assignments @db))]
    (swap! db assoc-in [:people person :duties] (str (count duties)))))

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
        (sort-by (fn [[duty-name {:keys [start-time]}]]
                   [start-time (string/lower-case duty-name)])
                 duties)))))

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

;; TODO: make this open a panel like settings
(defn help []
  [:div.btn-group
   [:button.btn.btn-default.dropdown-toggle
    {:data-toggle "dropdown"
     :aria-expanded "false"}
    [:span.glyphicon.glyphicon-question-sign {:aria-hidden "true"}]]
   [:div.panel.panel-default.dropdown-menu.dropdown-menu-right
    {:style {:width "550px"}}
    [:div.panel-body
     [:ul.list-unstyled
      [:li "Add data, make a schedule"]]]]])

;; TODO: move to shared

(defn save-file [filename t s]
  (if js/Blob
    (let [b (js/Blob. #js [s] #js {:type t})]
      (if js/window.navigator.msSaveBlob
        (js/window.navigator.msSaveBlob b filename)
        (let [link (js/document.createElement "a")]
          (aset link "download" filename)
          (if js/window.webkitURL
            (aset link "href" (js/window.webkitURL.createObjectURL b))
            (do
              (aset link "href" (js/window.URL.createObjectURL b))
              (aset link "onclick" (fn destroy-clicked [e]
                                     (.removeChild (.-body js/document) (.-target e))))
              (aset link "style" "display" "none")
              (.appendChild (.-body js/document) link)))
          (.click link))))
    (log/error "Browser does not support Blob")))

(defn ends-with [s suffix]
  (not (neg? (.indexOf s suffix (- (.-length s) (.-length suffix))))))

(defn read-file [r file deserialize]
  (if js/FileReader
    (let [reader (js/FileReader.)]
      (set! (.-onload reader)
            (fn csv-loaded [e]
              (when-let [new-graph (deserialize (.. e -target -result))]
                (reset! r new-graph))))
      (.readAsText reader file))
    (js/alert "Browser does not support FileReader")))

(defn filename [{:keys [title]} ext]
  (str (or title "roster") "." ext))

(defn import-button [label accept deserialize db]
  [:li
   [:a.btn.btn-file
    label
    [:input
     {:type "file"
      :name "import"
      :tab-index "-1"
      :accept accept
      :value ""
      :on-change
      (fn import-csv-change [e]
        (when-let [file (aget e "target" "files" 0)]
          (if (ends-with (.-name file) ".txt")
            (reset! db (read-file db file deserialize))
            (log/error "Must supply a .dot or .txt file"))))}]]])

(defn action-button [label f]
  [:li [:a.btn {:on-click f} label]])

(defn toolbar []
  [:div.btn-toolbar.pull-right {:role "toolbar"}
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Load"]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Empty"
      (fn clear-click [e]
        (reset! db {}))]
     [action-button "Simple Example"
      (fn random-click [e]
        (set-simple-example! db))]
     [action-button "Example"
      (fn random-click [e]
        (set-example! db))]
     [import-button "File (txt)" ".txt" edn/read-string db]]]
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Save"]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Summary table (txt)"
      (fn export-csv-click [e]
        (save-file (filename @db "txt") "text/csv" (pr-str @db)))]]]
   [help]])

(defn roster-page []
  (let [{:keys [people duties roles rules assignments]} @db]
    [:div
     [toolbar]
     [:br]
     [:button
      {:on-click
       (fn [e]
         (solve! solve-randomly))}
      "Randomize"]
     [:button
      {:on-click
       (fn [e]
         (solve! solve-csp))}
      "Randomize (CSP)"]
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
