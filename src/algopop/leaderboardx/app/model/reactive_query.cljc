(ns algopop.leaderboardx.app.model.reactive-query
  (:require [datascript.core :as d]
            [justice.core :as j]
            #?(:cljs [reagent.core :as reagent])
            #?(:cljs [reagent.ratom :as ratom])))

;; TODO: determine if datoms in the TX would actually affect the query
(defn matches [query tx-report]
  true)

#?(:cljs
   (defn rq
     "Creates a reactive query.
     Takes justice syntax query and returns 2 derefable things [results db].
     Returning the db is necessary to detect change,
     as entities with the same id are equal regardless of their attributes."
     [query]
     (let [k (keyword (gensym "listener_"))
           reactive-db (reagent/atom @j/*conn*)]
       (d/listen! j/*conn* k
                  (fn tx-listener [tx-report]
                    (when (matches query tx-report))
                    (reset! reactive-db (:db-after tx-report))))
       [(ratom/make-reaction
         (fn [] (j/q @reactive-db query))
         :on-dispose (fn dispose-rq []
                       (d/unlisten! j/*conn* k)))
        reactive-db])))

#?(:clj
   (defn rq [query]
     ;; TODO: what does this mean in Clojure?
     ))
