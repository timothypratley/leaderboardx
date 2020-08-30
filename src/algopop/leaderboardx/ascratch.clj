(ns algopop.leaderboardx.ascratch
  (:require [meander.substitute.syntax.epsilon :as ss]
            [meander.substitute.epsilon :as s]
            [meander.epsilon :as m]))

(defn substitution [pattern]
  (-> (ss/parse pattern)
      (s/compile {})))

(def ^{:arglists '([!titles !values])} ff
  (eval
   `(fn [~'!titles ~'!values]
      ~(substitution
        '[:div.form-inline
          [:table.table.table-responsive.panel.panel-default
           [:thead . [:tr . [:td !titles] ...]]
           [:tbody . [:tr . [:td !values] ...] ...]]]))))

(defn make-sub [args p]
  (eval (list `fn args (-> (ss/parse p) (s/compile {})))))

(make-sub
 '[!titles !values]
 '[:div.form-inline
   [:table.table.table-responsive.panel.panel-default
    [:thead . [:tr . [:td !titles] ...]]
    [:tbody . [:tr . [:td !values] ...] ...]]])

(ff ["A" "B" "C"] [1 2 3])

(defn entity-editor [?heading entities add-entity remove-entity add-attribute remove-attribute schema]
  (let [!titles (keys entities)
        !values (mapcat vals (vals entities))]
    (s/substitute
     [:div.form-inline
      [:h3 ?heading]
      [:table.table.table-responsive.panel.panel-default
       [:thead . [:tr . [:td !titles] ...]]
       [:tbody . [:tr . [:td !values] ...] ...]]])))
