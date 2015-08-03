(ns algopop.leaderboardx.app.views.details
  (:require [reagent.core :as reagent]
            [reagent.session :as session]
            [clojure.string :as string]
            [clojure.set :as set]
            [algopop.leaderboardx.app.views.common :as common]))

(def data
  (reagent/atom {:hair "red"
                 :shape "circle"
                 :nested {:more "data"
                          :and "stuff"}}))

(defn submit [k e editing]
  (let [{:keys [x]} (common/form-data e)
        [prev pos] @editing
        x (if (= pos :k)
            (string/trim x)
            (map string/trim (string/split x #"[,;]")))]
    (if (= pos :k)
      (swap! data set/rename-keys {k x})
      (swap! data assoc k x))
    (reset! editing nil)))

(defn input [x editing]
  (reagent/create-class
   {:display-name "node-input"
    :component-did-mount common/focus-append
    :reagent-render
    (fn node-input-render [x editing]
      [:input.form-control
       {:type "text"
        :name "x"
        :style {:width "100%"}
        :default-value x
        :on-blur (fn node-input-blur [e]
                   (reset! editing nil))}])}))

(declare render)

(defn render-nested-map [m depth]
  (into
   [:ul.list-unstyled]
   (for [[k v] m]
     [:li
      (when (> depth 2)
        {:style {:padding-left "40px"}})
      [(keyword (str "h" (inc depth))) [render k (inc depth)]]
      [render v (inc depth)]])))

(defn render-map [m depth]
  (into
   [:ul.list-unstyled]
   (for [[k v] m]
     [:li.row
      [:div.col-xs-4
       {:style {:text-align "right"}}
       [render k (inc depth)]]
      [:div.col-xs-7
       [render v (inc depth)]]
      [:div.col-xs-1
       [:button.btn.btn-default
        {:on-click (fn nest-click [e]
                     (swap! data assoc k {"foo" "bar"}))}
        "nest"]]])))

(defn render-seq [xs depth]
  (into [:ul.list-unstyled]
        (for [x xs]
          [:li [render x depth]])))

(defn render [x depth]
  [:span
   (cond
     (map? x) (if (= 1 depth)
                [render-map x depth]
                [render-nested-map x depth])
     (string? x) x
     (keyword? x) (name x)
     (seq? x) [render-seq x depth]
     :else (str x))])

(defn details [x]
  [render x 1])

(defn details-view []
  [:div
   [:form.row
    {:on-submit (fn details-submit [e]
                  (let [{:keys [k v]} (common/form-data e)]
                    (swap! data assoc k v)))}
    [:div.col-xs-4
     [:input.form-control {:type "text"
                           :name "k"}]]
    [:div.col-xs-7
     [:input.form-control {:type "text"
                           :name "v"}]]
    [:div.col-xs-1
     [:input {:type "submit"}]]]
   [details (session/get :model)]])
