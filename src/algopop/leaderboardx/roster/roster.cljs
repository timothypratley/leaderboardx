(ns algopop.leaderboardx.roster.roster
  (:require [algopop.leaderboardx.app.io.file :as file]
            [algopop.leaderboardx.app.logging :as log]
            [algopop.leaderboardx.app.views.db-entity-editor :as ee]
            [algopop.leaderboardx.roster.solve :as schedule]
            [algopop.leaderboardx.roster.seed :as seed]
            [algopop.leaderboardx.roster.table-view :as table-view]
            [justice.core :as j]
            [reagent.core :as reagent]))

;; TODO: schema can define relations -> dropdown

(defn solve! [solver]
  (let [assignments (solver)]
    (j/transacte (for [[[day duty-name] people] assignments
                       person-name people]
                   #:edge{:name (str person-name " -> " day ":" duty-name)
                          :from [:person/name person-name]
                          :to [:duty/name duty-name]
                          :day day}))))

(defn filename [{:keys [title]} ext]
  (str (or title "roster") "." ext))

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
          (if (file/ends-with (.-name file) ".txt")
            (reset! db (file/read-file db file deserialize))
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
     [action-button "Empty" (fn clear-click [e]
                              ;; TODO:
                              )]
     [action-button "Simple Example"
      (fn random-click [e]
        (seed/set-simple-example!))]
     [action-button "Example"
      (fn random-click [e]
        #_(seed/set-example! db))]
     #_[import-button "File (txt)" ".txt" edn/read-string db]]]
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Save"]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Summary table (txt)"
      (fn export-csv-click [e]
        #_(file/save-file (filename @db "txt") "text/csv" (pr-str @db)))]]]
   [help]])

;; remove
(def db (reagent/atom {}))

(defn roster-page []
  [:div
   [toolbar]
   [:br]
   [:button
    {:on-click
     (fn on-click-randomize [e]
       (solve! schedule/solve-randomly))}
    "Randomize"]
   [:button
    {:on-click
     (fn on-click-randomize-csp [e]
       (solve! schedule/solve-csp))}
    "Randomize (CSP)"]
   [table-view/roster db]
   [table-view/roster-summary db]
   [:div.well {:style {:background-color "white"}}
    [ee/db-entity-editor "person" "People" '{:person/name _}]]
   [:div.well {:style {:background-color "white"}}
    [ee/db-entity-editor "role" "Roles" '{:role/name _}]]
   [:div.well {:style {:background-color "white"}}
    [ee/db-entity-editor "duty" "Duty" '{:duty/name _}]]])
