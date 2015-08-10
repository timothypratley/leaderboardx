(ns algopop.leaderboardx.app.views.assess
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.seed :as seed]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [clojure.string :as string]))

(defn metrics [path model editing title ks]
  [:div
   [:label "Metrics"]
   (into
    [:div.row]
    (let [size (quot 12 (count ks))]
      (for [k ks]
        [:div.form-group
         {:class (str "col-xs-" size)}
         [:label.control-label k]
         [:input.form-control
          {:id k
           :type "number"}]])))])

(def conjv (fnil conj []))

(defn ol [path model editing title]
  [:div
   [:label title]
   (-> [:ul]
       (into
        (map-indexed
         (fn [idx line]
           [:li
            [common/editable-string (conj path idx) model editing]])
         (get-in @model path)))
       (conj
        [:i.small
         {:on-click
          (fn an-ol-add-click [e]
            (reset! editing (conj path (count (get-in @model path))))
            (swap! model update-in path conjv ""))}
         "+ Add"]))])

(defn textarea [path model editing title]
  [:div.form-group
   [:label title]
   [:textarea.form-control
    {:spellCheck "true"}]])

(defn unknown [x]
  [:div (pr-str x)])

(def dispatch
  {:metrics metrics
   :ol ol
   :textarea textarea})

(defn fc [t path model editing data]
  (apply (dispatch t unknown)
         path model editing data))

;; TODO: bind-ffirst?
(defn assess-view []
  (let [ac (db/assessment-components)
        assess (or (session/get :assess)
                  (:assess (session/put! :assess (reagent/atom {}))))
        g (or (session/get :graph)
              (:graph (session/put! :graph (reagent/atom seed/example))))
        ;; TODO: make a better session
        selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        editing (or (session/get :editing)
                    (:editing (session/put! :editing (reagent/atom nil))))]
    (fn an-assess-view []
      [:div
       [:h1 [common/selectable [] selected-id editing (vec (keys (:nodes @g)))]]
       (into
        [:div]
        (for [[t & data] (ffirst @ac)]
          [fc t (into [@selected-id] data) assess editing data]))
       [:div
        [:button.btn.btn-default
         "Save"]
        [:button.btn.btn-default
         "Schedule"]
        [:button.btn.btn-default
         "Publish"]]])))
