(ns algopop.leaderboardx.app.views.common
  (:require [goog.dom.forms :as forms]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [cljs.test :as t :include-macros true :refer-macros [testing is]]
            [devcards.core :as dc :refer-macros [defcard deftest]]
            [cljs.tools.reader.edn :as edn]))

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
    (write b)))

(defn editable-string [default-value write attrs input-type]
  (let [visible-value (reagent/atom default-value)
        current-default (reagent/atom default-value)]
    (fn an-editable-string [default-value write attrs input-type]
      (when (not= default-value @current-default)
        (reset! current-default default-value)
        (reset! visible-value default-value))
      [:input
       (merge-with
         merge
         ;; TODO: what about decimal numbers?
         {:type input-type
          :style {:width "100%"
                  :border "1px solid #f0f0f0"
                  :background-color (if (= default-value @visible-value)
                                      "white"
                                      "#f8f8f8")}
          :value @visible-value
          :on-change
          (fn editable-string-change [e]
            (reset! visible-value
                    ;; TODO: is this the right way to get numbers? /shrug seems to work
                    (cond-> (.. e -target -value)
                      (= input-type "number") (js/parseFloat))))
          :on-blur
          (fn editable-string-blur [e]
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
              nil))}
         attrs)])))

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

(defn selectable [default-value write values labels]
  (into
    [:select
     {:value (pr-str default-value)
      :on-change
      (fn selection-change [e]
        (save write default-value (edn/read-string (.. e -target -value))))}]
    (for [[value label] (map vector values (or labels (map str values)))]
      [:option {:value (pr-str value)} label])))

(defn add [label write]
  (let [show? (reagent/atom false)]
    (fn an-add [label write]
      (if @show?
        [editable-string
         ""
         (fn [v]
           (swap! show? not)
           (write v))
         {:auto-focus true
          :style {:text-align "right"}}]
        [:button.btn.btn-default.btn-sm
         {:style {:width "100%"}
          :on-click
          (fn add-click [e]
            (swap! show? not))}
         label]))))

(defn single-entity-editor [id entity title add-attribute remove-attribute schema]
  ;; TODO: make collapsable??
  (let [just-added (reagent/atom nil)]
    (fn a-single-entity-editor [id entity title add-attribute remove-attribute schema]
      [:div.form-inline
       [:table.table.table-responsive.panel.panel-default
        [:thead
         [:tr
          [:td {:col-span 3}
           [:h3 [:i title]]]]]
        [:tbody
         (doall
           (for [[attribute value] (sort entity)
                 :let [options (get schema attribute)]
                 :when (not= options :hide)]
             ^{:key attribute}
             [:tr
              [:td
               {:style {:font-weight "bold"
                        :width "40%"
                        :text-align "right"}}
               attribute ":"]
              [:td
               {:style {:width "60%"}}
               (cond
                 (seq? options)
                 (if (<= (count options) 1)
                   [:div value]
                   [selectable value #(add-attribute id attribute %) options])

                 (= options :number)
                 [editable-string value #(add-attribute id attribute %)
                  {:auto-focus (= attribute @just-added)}
                  "number"]

                 :else
                 [editable-string value #(add-attribute id attribute %)
                  {:auto-focus (= attribute @just-added)}])]
              [:td
               [:button.close
                {:on-click
                 (fn click-clear-attribute [e]
                   ;; TODO: currently you can remove a node/type! that seems wrong...
                   ;; maybe... but it works? maybe not a bad thing?
                   (remove-attribute id attribute))}
                "×"]]]))
         [:tr
          [:td
           {:style {:text-align "right"
                    :width "40%"}}
           [add "Add attribute"
            (fn click-add-attribute [x]
              ;; TODO: entities might not be either nodes or edges?
              (let [attribute (keyword (if (vector? id) "edge" "node") x)]
                (reset! just-added attribute)
                (add-attribute id attribute "")))]]
          [:td]
          [:td]]]]])))

;; TODO: idea - have 3 text boxes, just like graph node entry but for entity/attrpairs
(defn entity-editor [heading entities add-entity remove-entity add-attribute remove-attribute schema]
  [:div
   [:h3 heading]
   [:ul.list-unstyled
    [:li.row
     [:div.col-xs-3
      {:style {:text-align "right"}}
      [add "Add" add-entity]]]
    (for [[entity-name entity] (sort entities)]
      ^{:key entity-name}
      [:li.row
       {:style {:padding "10px"}}
       [:div.col-xs-11 [single-entity-editor entity-name entity entity-name add-attribute remove-attribute schema]]
       [:div.col-xs-1
        {:style {:text-align "right"}}
        [:button.close
         {:style {:float "left"}
          :on-click
          (fn [e]
            (remove-entity entity-name))}
         "×"]]])]])
