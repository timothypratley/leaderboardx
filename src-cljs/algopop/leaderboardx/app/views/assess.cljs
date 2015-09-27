(ns algopop.leaderboardx.app.views.assess
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.commands :as commands]
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
           :type "number"
           :on-focus
           (fn metric-focus [e]
             (reset! editing (conj path k)))
           :on-change
           (fn metric-change [e]
             (swap! model assoc-in (conj path k)
                    (.. e -target -value)))}]])))])

(def conjv (fnil conj []))

(defn ol [path model editing title]
  [:div
   [:label title]
   (-> [:ul]
       (into
        (map-indexed
         (fn a-li [idx line]
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

(defn attribute [t path model editing data]
  (apply (dispatch t unknown)
         path model editing data))

;; TODO: bind-ffirst?
(defn assess-view [{:keys [name date]}]
  (let [ac (db/assessment-components)
        assess (or (session/get :assess)
                  (:assess (session/put! :assess (reagent/atom {:name "New"}))))
        g (or (session/get :graph)
              (:graph (session/put! :graph (reagent/atom seed/example))))
        nodes (db/watch-nodes)
        ;; TODO: make a better session
        selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        editing (or (session/get :editing)
                    (:editing (session/put! :editing (reagent/atom nil))))]
    (fn an-assess-view []
      [:div
       [:h1
        [common/editable-string assess [:name] editing]
        [:span " - "]
        [common/selectable [] selected-id editing (mapv :node/name (vals @nodes))]]
       (into
        [:div]
        (for [[type & data] (ffirst @ac)]
          [attribute type (into [@selected-id] data) assess editing data]))
       [:div
        [:button.btn.btn-default
         {:on-click
          (fn a-save-click [e]
            (commands/save @assess))}
         "Save"]
        [:button.btn.btn-default
         "Schedule"]
        [:button.btn.btn-default
         "Publish"]]])))

(defn assessments-view []
  [:div
   [:ul
    [:li [:a {:href "/#/assess/Tim/fitness/14-Sept-2015"} "one"]]]])
