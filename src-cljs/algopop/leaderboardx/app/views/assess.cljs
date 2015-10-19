(ns algopop.leaderboardx.app.views.assess
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.commands :as commands]
            [algopop.leaderboardx.app.seed :as seed]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [cljs.test :as t :include-macros true :refer-macros [testing is]]
            [clojure.string :as string]
            [devcards.core :as dc :refer-macros [defcard deftest]]))

(defn attr2title [attr]
  (string/capitalize (string/join " " (string/split (name attr) "-"))))

(defn title2attr [title]
  (keyword "assessment" (string/replace (string/lower-case title) " " "-")))

(deftest titles-test
  (is (= :assessment/group-hug-please
         (title2attr "Group hug please")))
  (is (= "Group hug please"
         (attr2title :assessment/group-hug-please))))

(defn group [title model path editing ks]
  (prn "Group ks" ks)
  [:div
   [:label title]
   (into
    [:div.row]
    (let [size (quot 12 (count ks))]
      (for [k ks]
        [:div.form-group
         {:class (str "col-xs-" size)}
         [:label.control-label (str k)]
         [:input.form-control
          {:id (str k)
           :type "number"
           :on-focus
           (fn group-focus [e]
             (reset! editing (conj path k)))
           :on-change
           (fn group-change [e]
             (swap! model assoc-in (conj path k)
                    (.. e -target -value)))}]])))])

(def conjv (fnil conj []))

(defn ol [title model path editing]
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

(defn textarea [title model path editing]
  [:div.form-group
   [:label title]
   [:textarea.form-control
    {:spellCheck "true"}]])

(defn unknown [x]
  [:div "Unknown:" x])

(defn select5 [title model path editing]
  [:div
   [:label title]
   [:select {:options [1 2 3 4 5]}]])

(def dispatch
  {"group" group
   "select5" select5
   "ol" ol
   "textarea" textarea})

(defn attribute [attr type model path editing data]
  (apply (dispatch type unknown)
         (attr2title attr) model path editing data))

;; TODO: bind-ffirst?
(defn assess-view [{:keys [name date]}]
  (let [ac (db/assessment-components "player-assessment")
        assess (or (session/get :assess)
                  (:assess (session/put! :assess (reagent/atom {:name "New"}))))
        nodes (db/watch-nodes)
        ;; TODO: make a better session
        selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        editing (or (session/get :editing)
                    (:editing (session/put! :editing (reagent/atom nil))))]
    (prn "AC" @ac)
    (fn an-assess-view [{:keys [name date]}]
      [:div
       [:h1
        [common/editable-string assess [:name] editing]
        [:span " - "]
        [common/selectable [] selected-id editing (mapv :node/name (vals @nodes))]]
       (into
        [:div]
        ;; TODO: name needs to be attr and name (for datomic)
        ;; can't have nested maps?
        (for [[attr type & data] (ffirst @ac)]
          [attribute attr type assess [name] editing data]))
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
