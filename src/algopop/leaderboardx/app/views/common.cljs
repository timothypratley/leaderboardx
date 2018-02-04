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
       (merge-with merge
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

(defn blur-active-input []
  (let [activeElement (.-activeElement js/document)]
    (when (some-> activeElement (.-tagName) #{"INPUT" "TEXTAREA"})
      (.blur activeElement))))

(defn save [write a b]
  (when (and write (not= a b))
    (write b))
  (blur-active-input))

(defn editable-string [default-value write]
  (let [focused (reagent/atom false)
        visible-value (reagent/atom default-value)]
    (fn an-editable-string [default-value write]
      (when (not @focused)
        (reset! visible-value default-value))
      [:input
       {:type "text"
        :style {:width "100%"
                :border "1px solid #f0f0f0"}
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
          (.stopPropagation e)
          (.stopImmediatePropagation (.-nativeEvent e))
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

;; TODO: move tests to another namespace to save production build size
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

(defn add [label write]
  (let [show? (reagent/atom false)]
    (fn an-add [label write]
      (if @show?
        [focus-append-input
         {:style {:width "100%"
                  :text-align "right"}
          :on-blur
          (fn editable-string-blur [e]
            (swap! show? not)
            (let [v (.. e -target -value)]
              (when (seq v)
                (write v))))
          :on-key-down
          (fn editable-string-key-down [e]
            (.stopPropagation e)
            (.stopImmediatePropagation (.-nativeEvent e))
            (case (key-code-name (.-keyCode e))
              "ESC" (swap! show? not)
              "ENTER" (do
                        (.preventDefault e)
                        (let [v (.. e -target -value)]
                          (when (and (seq v) write)
                            (write v)))
                        (swap! show? not))
              nil))}]
        [:button.btn.btn-default.btn-sm
         {:style {:width "100%"}
          :on-click
          (fn add-click [e]
            (swap! show? not))}
         label]))))

(defn single-entity-editor [entity-name entity add-attribute remove-attribute]
  [:div.form-inline
   [:table.table.table-responsive
    [:tbody
     (doall
       (for [[attribute value] (sort entity)]
         ^{:key attribute}
         [:tr
          [:td
           {:style {:font-weight "bold"
                    :width "40%"
                    :text-align "right"}}
           attribute ":"]
          [:td
           {:style {:width "60%"}}
           [editable-string value #(add-attribute entity-name attribute %)]]
          [:td [:button.close
                {:on-click
                 (fn [e]
                   (remove-attribute entity-name attribute))}
                "×"]]]))
     [:tr
      [:td
       {:style {:text-align "right"}}
       [add "Add attribute" #(add-attribute entity-name (keyword %) "")]]]]]])

(defn entity-editor [heading entities add-entity remove-entity add-attribute remove-attribute]
  [:div
   [:h3 heading]
   [:ul.list-unstyled
    [:li.row
     [:div.col-xs-3
      {:style {:text-align "right"}}
      [add (str "Add " heading) add-entity]]]
    (for [[entity-name entity] (sort entities)]
      ^{:key entity-name}
      [:li.row
       {:style {:padding "10px"}}
       [:div.col-xs-3
        {:style {:text-align "right"}}
        [:button.close
         {:style {:float "left"}
          :on-click
          (fn [e]
            (remove-entity entity-name))}
         "×"]
        [:strong entity-name]]
       [:div.col-xs-9 [single-entity-editor entity-name entity add-attribute remove-attribute]]])]])
