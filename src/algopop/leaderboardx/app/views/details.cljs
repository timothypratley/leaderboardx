(ns algopop.leaderboardx.app.views.details
  (:require
    [reagent.core :as reagent]
    [reagent.session :as session]
    [clojure.string :as string]
    [clojure.set :as set]
    [algopop.leaderboardx.app.db :as db]
    [algopop.leaderboardx.app.views.common :as common])
  (:require-macros
    [reagent.ratom :as ratom]))

(def data
  (reagent/atom {:name "unknown"}))

(declare render)

(defn render-nested-map [m path model editing]
  (into
   [:ul.list-unstyled]
   (for [[k v] m]
     [:li
      (when (pos? (count path))
        {:style {:padding-left "40px"}})
      [(keyword (str "h" (+ 2 (count path))))
       (name k)]
      [render (conj path k) model editing]])))

(defn render-map [m path model editing]
  (into
   [:ul.list-unstyled]
   (for [[k {:keys [node/name] :as v}] m]
     [:li.row
      [:div.col-xs-4
       {:style {:text-align "right"}}
       name]
      [:div.col-xs-6
       [render (conj path k) model editing]]
      [:div.col-xs-2
       [:button.btn.btn-default.form-control
        {:on-click
         (fn nest-click [e]
           ;; TODO: putting v in the path is wierd
           (swap! model assoc-in (conj path
                                       (if (map? v)
                                         (string/join "," (keys v))
                                         v)) {}))}
        "nest"]]])))

(defn render-seq [xs path model editing]
  (into [:ul.list-unstyled]
        (map-indexed
         (fn a-li [idx x]
           [:li [render
                 (if (set? xs)
                   (conj path x)
                   (conj path idx))
                 model
                 editing]])
         xs)))

(defn render [path model editing]
  (let [x (get-in @model path)]
    [:span
     (cond
       (map? x) (if (seq path)
                  [render-nested-map x path model editing]
                  [render-map x path model editing])
       (string? x) [common/editable-string model path editing]
       (keyword? x) (name x)
       (seq? x) [render-seq x path model editing]
       :else (str x))]))

(defn details [x]
  (let [editing (reagent/atom nil)
        model (ratom/reaction
               (into {} @x))]
    (fn a-details-view [x]
      [render [] model editing])))

(defn details-view []
  (let [w (db/watch-nodes)]
    (fn a-details-view []
      [:div
       [:form.row
        {:on-submit (fn details-submit [e]
                      (let [{:keys [k v]} (common/form-data e)]
                        (db/add-node v)
                        (swap! data assoc k v)))}
        [:div.col-xs-4
         [:input.form-control
          {:type "text"
           :name "k"}]]
        [:div.col-xs-6
         [:input.form-control
          {:type "text"
           :name "v"}]]
        [:div.col-xs-2
         [:input.form-control
          {:type "submit"}]]]
       [details w]])))
