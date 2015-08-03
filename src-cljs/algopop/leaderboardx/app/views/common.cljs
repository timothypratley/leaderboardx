(ns algopop.leaderboardx.app.views.common
  (:require [goog.dom.forms :as forms]
            [reagent.core :as reagent]
            [datascript :as d]
            [cljs-uuid.core :as uuid]))

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

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [e]
  (.preventDefault e)
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn editable-line-input [default-value editing]
  (reagent/create-class
   {:display-name "editable line"
    :component-did-mount focus-append
    :reagent-render
    (fn editable-line-input-render [default-value editing]
      [:input
       {:type "text"
        :on-key-down (fn editable-key-down [e]
                       (println (.-keyCode e)))
        :style {:width "100%"}
        :default-value default-value
        :on-blur (fn node-input-blur [e]
                   (reset! editing nil))}])}))

(defn editable-line [default-value]
  (let [editing (reagent/atom nil)]
    (fn an-editable-line [default-value]
      [:li {:on-click (fn span-click [e]
                        (reset! editing true)
                        e)}
       (if @editing
         [editable-line-input default-value editing]
         default-value)])))

(defn bind
  ([conn q]
   (bind conn q (reagent/atom nil)))
  ([conn q state]
   (let [k (uuid/make-random)]
     (reset! state (d/q q @conn))
     (d/listen! conn k (fn [tx-report]
                         (reset! state (d/q q (:db-after tx-report)))))
     (set! (.-__key state) k)
     state)))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))
