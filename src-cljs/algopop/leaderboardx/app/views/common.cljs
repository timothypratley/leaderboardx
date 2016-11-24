(ns algopop.leaderboardx.app.views.common
  (:require [goog.dom.forms :as forms]
            [reagent.core :as reagent]
            [datascript.core :as d]))

;; TODO: doesn't belong here

(defn focus-append [this]
  (doto (.getDOMNode this)
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

(defn save [e path model editing]
  (.preventDefault e)
  (swap! model assoc-in path (.-value (.-target e)))
  (reset! editing nil))

(defn editable-string [path model editing]
  (if (= path @editing)
    [focus-append-input
     {:default-value (get-in @model path)
      :on-blur
      (fn editable-string-blur [e]
        (save e path model editing))
      :on-key-down
      (fn editable-string-key-down [e]
        (case (key-code-name (.-keyCode e))
          "ESC" (reset! editing nil)
          "ENTER" (save e path model editing)
          nil))}]
    [:span.editable
     {:style {:width "100%"}
      :on-click
      (fn editable-string-click [e]
        (reset! editing path))}
     (get-in @model path)
     [:span.glyphicon.glyphicon-pencil.edit]]))

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

(defn bind
  ([conn q]
   (bind conn q (reagent/atom nil)))
  ([conn q state]
   (let [k (random-uuid)]
     (reset! state (d/q q @conn))
     (d/listen! conn k (fn [tx-report]
                         (reset! state (d/q q (:db-after tx-report)))))
     (set! (.-__key state) k)
     state)))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))
