(ns algopop.leaderboardx.app.views.common
  (:require [goog.dom.forms :as forms]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [cljs.test :as t :include-macros true :refer-macros [testing is]]
            [devcards.core :as dc :refer-macros [defcard deftest]]
            [clojure.string :as string]))

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

(defn save [write a b]
  (when (and write (not= a b))
    (write b)))

(defn blur-active-input []
  (let [activeElement (.-activeElement js/document)]
    (when (some-> activeElement (.-tagName) #{"INPUT" "TEXTAREA"})
      (.blur activeElement))))

(defn editable-string [default-value write]
  (let [focused (reagent/atom false)
        visible-value (reagent/atom default-value)]
    (fn an-editable-string [default-value write]
      (when (not @focused)
        (reset! visible-value default-value))
      [:input
       {:type "text"
        :style {:width "100%"}
        :value @visible-value
        :on-change
        (fn editable-string-change [e]
          (reset! visible-value (.. e -target -value)))
        :on-focus
        (fn editable-string-focus [e]
          (reset! focused true))
        :on-blur
        (fn editable-string-blur [e]
          (reset! focused false)
          (save write default-value @visible-value))
        :on-key-down
        (fn editable-string-key-down [e]
          (case (key-code-name (.-keyCode e))
            "ESC" (do
                    (reset! visible-value default-value)
                    (blur-active-input))
            "ENTER" (do
                      (.preventDefault e)
                      (save write default-value @visible-value))
            nil))}])))

(defcard editable-string-example
  "editable string"
  (dc/reagent [editable-string "foo" (fn [x] x)]))

(deftest some-test
  "blah blah blah"
  (testing "zzz"
    (is (= 1 2) "nah")
    (is (= 1 1) "obviously")))

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [e]
  (.preventDefault e)
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

;; TODO: does this need editing?
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

(defn add [write]
  (let [show? (reagent/atom false)]
    (fn an-add [write]
      (if @show?
        [focus-append-input
         {:style {:width "100%"}
          :on-blur
          (fn editable-string-blur [e]
            (let [v (.. e -target -value)]
              (when (seq v)
                (write v))))
          :on-key-down
          (fn editable-string-key-down [e]
            (case (key-code-name (.-keyCode e))
              "ESC" (swap! show? not)
              "ENTER" (do
                        (let [v (.. e -target -value)]
                          (when (seq v)
                            (write v)))
                        (swap! show? not))
              nil))}]
        [:button
         {:on-click
          (fn add-click [e]
            (swap! show? not))}
         "Add"]))))

(defn entity-editor [heading entities add-entity remove-entity add-attribute remove-attribute]
  [:div
   [:h3 heading]
   [:ul.list-unstyled
    (for [[entity-name entity] (sort entities)]
      ^{:key entity-name}
      [:li.row
       {:style {:padding "10px"}}
       [:div.col-xs-2
        {:style {:text-align "right"}}
        [:h4 entity-name [:a {:href (str "/#/graphs/" entity-name)} "π"]]
        [:button.remove
         {:on-click
          (fn [e]
            (remove-entity entity-name))}
         "x"]]
       [:div.col-xs-10
        [:div
         [:ul.list-unstyled
          (for [[attribute value] (sort entity)]
            ^{:key attribute}
            [:li.row
             [:div.col-xs-2
              {:style {:font-weight "bold"
                       :text-align "right"}}
              [editable-string attribute
               (fn [x]
                 ;; TODO: might combine into one update?
                 (add-attribute entity-name x value)
                 (remove-attribute entity-name attribute))]
              [:button.remove
               {:on-click
                (fn [e]
                  (remove-attribute entity-name attribute))}
               "x"]]
             [:div.col-xs-10
              [editable-string value #(add-attribute entity-name attribute %)]]])
          [:li.row
           [:div.col-xs-2
            {:style {:text-align "right"}}
            [add #(add-attribute entity-name % "")]]]]]]])
    [:li.row
     [:div.col-xs-2
      {:style {:text-align "right"}}
      [add add-entity]]]]])
