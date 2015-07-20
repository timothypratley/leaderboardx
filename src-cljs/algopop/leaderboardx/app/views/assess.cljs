(ns algopop.leaderboardx.app.views.assess
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.db :as db]
            [clojure.string :as string]))

(defn metrics [title ks]
  (into [:div
         [:h3 "Metrics"]]
        (for [k ks]
          [:div.form-group
           [:label.col-xs-2.control-label {:for k} k]
           [:div.col-xs-2
            [:input.form-control {:id k
                                  :type "number"}]]])))

(defn ol [title lines add-fn]
  [:div
   [:button.btn.btn-default.pull-right
    {:on-click (fn an-add-click [e]
                 (db/insert))}
    "Add "
    [:span.glyphicon.glyphicon-plus]]
   [:h3 title]
   (into [:ol]
         (for [line lines]
           [common/editable-line "Some default text"]))])

(defn textarea [title]
  [:div
   [:h3 title]
   [:textarea {:spellCheck "true"}]])

(defn unknown [x]
  [:div (pr-str x)])

(def dispatch
  {:metrics metrics
   :ol ol
   :textarea textarea})

(defn fc [t data]
  (apply (dispatch t unknown) data))

;; TODO: bind-ffirst?
(defn assess-view []
  (let [ac (db/assessment-components)]
    (fn an-assess-view []
      (into [:form.form-inline.form-horizontal]
            (for [[t & data] (ffirst @ac)]
              [fc t data])))))
