(ns algopop.leaderboardx.app.views.graph
  (:require [algopop.leaderboardx.app.views.d3 :as d3]
            [goog.dom.forms :as forms]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(defn rand-char []
  (char (+ 97 (rand-int 26))))

(def vowel? #{\a \e \i \o \u})

(defn next-char [c]
  (let [nxt (rand-char)]
    (if (or (vowel? c) (vowel? nxt))
      nxt
      (recur c))))

(defn rand-name []
  (apply str (take (+ 3 (rand-int 3))
                   (iterate next-char (rand-char)))))

(def test-graph
  (let [ks (distinct (repeatedly 10 rand-name))
        nodes (into {} (for [k ks]
                         [k {:hair (rand-nth ["red" "brown" "black" "blonde"])}]))]
    {:nodes nodes
     :edges (into {} (for [k ks]
                       [k (into {} (for [x (remove #{k} (take (+ 2 (rand-int 2)) (shuffle ks)))]
                                     [x {:value 1}]))]))}))

(defonce g (reagent/atom test-graph))

(defn merge-left [& maps]
  (apply merge (reverse maps)))

;; TODO: move server side but keep an action list to fast UI update
(defn add-em [g source targets]
  (-> g
      (update-in [:nodes] merge-left {source {}} (zipmap targets (repeat {})))
      (update-in [:edges source] merge-left (zipmap targets (repeat {})))))

(defn form-data [form]
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap form)))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn submit [e]
  (.preventDefault e)
  (let [{:keys [source targets]} (form-data (.-target e))]
    (swap! g add-em (string/trim source) (map string/trim (string/split targets #",")))))

(defn handle-resize [e]
  (println "RESIZE" e))

(defn delete-node [g id]
  (-> g
      (update-in [:nodes] dissoc id)
      (assoc :edges
             (into {}
                   (for [[k links] (dissoc (:edges g) id)]
                     [k (dissoc links id)])))))

(defn delete-edge [g [from to]]
  (println from to "EDGE")
  (update-in g [:edges from] dissoc to))

(defn handle-keydown [e]
  (case (.-keyCode e)
    46 (do
         (println "DELETE" @d3/selected-id "!")
         (when @d3/selected
           (if (string? @d3/selected-id)
             (swap! g delete-node @d3/selected-id)
             (swap! g delete-edge @d3/selected-id))))
    (.log js/console "KEYDOWN" e)))

;; metadata gah, riddiculous
(def hook
  (with-meta (fn [])
    {:component-did-mount
     (fn did-mount [this]
       (.addEventListener js/document "keydown" handle-keydown)
       (.addEventListener js/window "resize" handle-resize))
     :component-will-unmount
     (fn will-unmount [this]
       (.removeEventListener js/document "resize" handle-resize)
       (.removeEventListener js/window "keydown" handle-keydown))}))

(defn graph-page []
  ;; TODO: pass in session instead
  [:div
   [hook]
   [:div.row
    [:form.col-md-6 {:on-submit submit}
     [:dl.dl-horizontal
      [:dt [:input {:type "text"
                    :name "source"}]]
      [:dd [:input {:type "text"
                    :name "targets"}]
       [:input {:type :submit
                :value "↩"}]]]
     (into [:dl.dl-horizontal]
           (apply concat
                  (for [[k v] (:nodes @g)]
                    [[:dt k]
                     [:dd (string/join ", " (keys (get-in @g [:edges k])))]])))]
    [:div.col-md-6
     [:ul.list-unstyled
      [:li "Enter a node name and press ENTER to add it."]
      [:li "Enter a comma separated list of nodes to link to and press ENTER to add them."]
      [:li "Select a node or edge by mouse clicking it and press DEL to delete it."]
      [:li "Drag nodes or edges around by click hold and move."]
      [:li "Double click to unpin nodes and edges."]]]]
   [d3/graph @g]])
