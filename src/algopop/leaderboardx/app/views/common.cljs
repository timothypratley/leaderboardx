(ns algopop.leaderboardx.app.views.common
  (:require [goog.dom.forms :as forms]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [cljs.test :as t :include-macros true :refer-macros [testing is]]
            [devcards.core :as dc :refer-macros [defcard deftest]]))

;; TODO: doesn't belong here

(defn focus-append [this]
  (doto (dom/dom-node this)
    (.focus)
    (.setSelectionRange 100000 100000)))

(defn focus-append-input [m]
  (reagent/create-class
   {:display-name "focus-append-component"
    :component-did-mount focus-append
    :reagent-render
    (fn focus-append-input-render [m]
      [:input
       (merge
        {:type "text"
         :name "text"
         :style {:width "100%"}}
        m)])}))

;; TODO: advanced mode (set/map-invert (js->clj KeyCodes))
(def key-code-name
  {13 "ENTER"
   27 "ESC"
   46 "DELETE"
   8 "BACKSPACE"})

(defn save [model path editing write e]
  (.preventDefault e)
  (write model path (.. e -target -value))
  (reset! editing nil))

(defn editable-string
  ([model path editing]
   (editable-string model path editing
                    (fn update-model [m p v]
                      (swap! m assoc-in p v))))
  ([model path editing write]
   (if (= path @editing)
     [focus-append-input
      {:default-value (get-in @model path)
       :on-blur
       (fn editable-string-blur [e]
         (save model path editing write e))
       :on-key-down
       (fn editable-string-key-down [e]
         (case (key-code-name (.-keyCode e))
           "ESC" (reset! editing nil)
           "ENTER" (save model path editing write e)
           nil))}]
     [:span.editable
      {:style {:width "100%"
               :cursor "pointer"}
       :on-click
       (fn editable-string-click [e]
         (reset! editing path))}
      (get-in @model path)
      [:span.glyphicon.glyphicon-pencil.edit]])))

(defcard editable-string-example
  "editable string"
  (dc/reagent (editable-string (reagent/atom {:foo "bar"}) [:foo] (reagent/atom nil))))

(deftest some-test
  "blah blah blah"
  (testing "zzz"
    (is (= 1 2) "nah")
    (is (= 1 1) "obviously")))

(defn bound-input
  [model path editing write]
  [:input
   {:style {:width "100%"}
    :default-value (get-in @model path)
    :on-blur
    (fn editable-string-blur [e]
      (save model path editing write e))
    :on-key-down
    (fn editable-string-key-down [e]
      (case (key-code-name (.-keyCode e))
        "ESC" (reset! editing nil)
        "ENTER" (save model path editing write e)
        nil))}])

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [e]
  (.preventDefault e)
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn selectable [path model editing options]
  (if (or (= @editing options)
          (not (get-in @model path)))
    (into
     [:select
      {:default-value (get-in @model path)
       :on-change
       (fn selection [e]
         (when-let [idx (.-selectedIndex (.-target e))]
           (if (seq path)
             (swap! model assoc-in path (options idx))
             (reset! model (options idx)))
           (reset! editing nil)))}]
     (for [x options]
       [:option x]))
    [:span
     {:on-click
      (fn selectable-click [e]
        (reset! editing options)
        nil)}
     (get-in @model path)]))
