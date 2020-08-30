(ns algopop.leaderboardx.app.views.db-entity-editor
  (:require [algopop.leaderboardx.app.model.db :as db]
            [algopop.leaderboardx.app.model.reactive-query :as rq]
            [algopop.leaderboardx.app.views.common :as common]
            [clojure.string :as str]
            [reagent.core :as reagent]
            [justice.core :as j]))

;; TODO: might want the identity ident
(defn db-entity-editor [entity-type heading q]
  (reagent/with-let [[entities db] (rq/rq q)
                     new-entity (reagent/atom {})
                     submit-counter (reagent/atom 0)
                     add-new-entity (fn []
                                      (db/add-entity @new-entity)
                                      (reset! new-entity {})
                                      (swap! submit-counter inc))
                     ;; TODO: attributes are highly cachable
                     ;; TODO: justice regex?
                     attributes (filter (fn [{:db/keys [ident]}]
                                          (= entity-type (namespace ident)))
                                        (j/q '{:db/ident _}))]
    @db ;; forces render if db changed (entity equality is by id only)
    `[:div.form-inline
      [:h3 ~heading]
      [:table.table.table-responsive.panel.panel-default
       [:thead
        [:tr ~@(for [{:db/keys [ident] :as a} attributes]
                 [:th ident])
         ~[:th
           ;; TODO: style in css?
           {:style {:padding-left "2px"
                    :padding-right "2px"}}
           [:button.close
            {:on-click
             (fn click-edit-attributes [x] x)}
            "+"]]]]
       [:tbody
        ~(for [{:keys [db/id] :as entity} @entities]
           `[:tr {:key ~id}
             ~@(for [{:db/keys [ident]} attributes
                     :let [v (db/vstr (get entity ident))
                           write #(if (str/blank? %)
                                    (db/retract-attribute id ident)
                                    ;; TODO: for refs use ref syntax and handle missing & sets
                                    (db/add-attribute id ident %))]]
                 [:td [common/editable-string v write {} "text" common/blur-active-input]])
             ~[:td [:button.close
                    {:on-click
                     (fn click-remove-entity [e]
                       ;; TODO: currently you can remove a node/type! that seems wrong...
                       ;; maybe... but it works? maybe not a bad thing?
                       (db/retract-entity entity))}
                    "×"]]])]
       [:tfoot
        [:tr {:key ~(deref submit-counter)}
         ~@(map-indexed
            (fn [idx {:db/keys [ident]}]
              [:td [common/editable-string (get @new-entity ident) #(swap! new-entity assoc ident %)
                    (if (and (= idx 0) (pos? @submit-counter)) {:auto-focus true} {}) "text"
                    add-new-entity]])
            attributes)
         ~[:td
           ;; TODO: style in css?
           {:style {:padding-left "2px"
                    :padding-right "2px"}}
           [:button.close
            {:on-click
             (fn click-add-entity [e]
               (add-new-entity))}
            "+"]]]]]]))

;; TODO: might want to take identity-ident for query, type, and link resolution
;; TODO: why does this query not work in cljs?
;; {:node/type [:type/name "Person"]}
;; {:node/type {:type/name "Person"}}
