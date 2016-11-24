(ns algopop.leaderboardx.app.views.assess
  (:require [algopop.leaderboardx.app.views.common :as common]
            [algopop.leaderboardx.app.db :as db]
            [algopop.leaderboardx.app.commands :as commands]
            [algopop.leaderboardx.app.seed :as seed]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [reagent.ratom :as ratom]
            [cljs.test :as t :include-macros true :refer-macros [testing is]]
            [clojure.string :as string]
            [devcards.core :as dc :refer-macros [defcard defcard-rg deftest]]))

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
  (prn "SUP FIX ME" (get-in @model path) ks)
  [:div
   [:label title]
   (into
    [:div.row]
    (let [size (quot 12 (count ks))]
      (for [[t ct] ks]
        ;; TODO
        ;;((dispatch t) t model (conj path x) ct)
        [:div.form-group
         {:class (str "col-xs-" size)}
         [:label.control-label (str t)]
         [:input.form-control
          {:id (str t)
           :type "number"
           :on-focus
           (fn group-focus [e]
             (reset! editing (conj path t)))
           :on-change
           (fn group-change [e]
             (swap! model assoc-in (conj path t)
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
            [common/editable-string model (conj path idx) editing]])
         (get-in @model path)))
       (conj
        [:div.small
         {:style {:width "100%"
                  :background "white"}
          :on-click
          (fn an-ol-add-click [e]
            (reset! editing (conj path (count (get-in @model path))))
            (swap! model update-in path conjv ""))}
         "+ Add"]))])

(defn textarea [title model path editing]
  [:div.form-group
   [:label title]
   [:textarea.form-control
    {:spellCheck "true"}]])

(defn unknown [type title]
  [:div (str "Unknown " type ": " title)])

(defn select5 [title model path editing]
  [:div
   [:label title]
   [:select {:options [1 2 3 4 5]}]])

(def dispatch
  {"group" group
   "select5" select5
   "ol" ol
   "textarea" textarea})

(defn attribute [type name model path editing children]
  ((dispatch type (partial unknown type))
   name model path editing children))

(defn attributes [[type name & children] assess names editing]
  (into
   [:div]
   (for [[type name & children] children]
     [attribute type name assess [name] editing children])))

(defn assess-form [template assess names editing selected-id]
  [:div
   [:h1
    [common/editable-string assess [:name] editing]
    [:span " - "]
    [common/selectable [:selected] names editing (:names @names)]]
   [attributes template assess names editing]
   [:div
    [:button.btn.btn-default
     {:on-click
      (fn a-save-click [e]
        (commands/save @assess))}
     "Save"]
    [:button.btn.btn-default
     "Schedule"]
    [:button.btn.btn-default
     "Publish"]]])

(defn assess-view [{:keys [name date]}]
  (let [ac (db/assessment-components "player-assessment")
        assess (or (session/get :assess)
                   (:assess (session/put! :assess (reagent/atom {:name "New"}))))
        nodes (db/watch-nodes)
        template (session/get :model)
        ;; TODO: should be a reaction
        names (ratom/reaction {:names (mapv :node/name @nodes)})
        ;; TODO: make a better session
        selected-id (or (session/get :selected-id)
                        (:selected-id (session/put! :selected-id (reagent/atom nil))))
        editing (or (session/get :editing)
                    (:editing (session/put! :editing (reagent/atom nil))))]
    (fn an-assess-form [x]
      (assess-form template assess names editing selected-id))))

(def assesment-template
  ["template" "player-assessment"
   ["name" "assesse"]
   ["group" "metrics"
    ["select5" "productivity"]
    ["select5" "leadership"]
    ["select5" "happiness"]]
   ["ol" "achievements"]
   ["ol" "weaknesses"]
   ["ol" "goach-goals"]
   ["ol" "player-goals"]
   ["textarea" "coach-comments"]
   ["textarea" "player-comments"]])

(defn assessment-example []
  (let [assess (reagent/atom {})
        selected-id (reagent/atom nil)
        editing (reagent/atom nil)
        names (reagent/atom {:names ["tim" "foo" "bar"]})]
    (fn an-assessment-example []
      [assess-form assesment-template assess names editing selected-id])))

(defcard-rg assessment-card
  [assessment-example])

(defn assessments-view []
  [:div
   [:ul
    [:li [:a {:href "/#/assess/Tim/fitness/14-Sept-2015"} "one"]]]])
