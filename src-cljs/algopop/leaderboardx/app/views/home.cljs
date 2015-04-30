(ns algopop.leaderboardx.app.views.home
  (:require [reagent.session :as session]))

(defn item [x]
  [:div
   [:button.btn.btn-default [:span.glyphicon.glyphicon-chevron-up]]
   [:button.btn.btn-default [:span.glyphicon.glyphicon-chevron-down]]
   x])

;; TODO: pass in session instead
(defn home-page []
  [:div
   (into [:ul.list-unstyled]
         (for [x ["usain" "jordan" "ronaldo"]]
           [:li [item x]]))])
