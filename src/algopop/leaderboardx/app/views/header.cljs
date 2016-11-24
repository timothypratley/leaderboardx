(ns algopop.leaderboardx.app.views.header
  (:require [algopop.leaderboardx.app.routes :as routes]
            [reagent.session :as session]))

;; TODO: what if empty?
(defn notifications []
  (when-let [e (session/get :errors)]
    [:li {:on-click (fn error-click [e]
                      (session/update-in! [:errors] next))}
     [:p.navbar-text
      [:span.glyphicon.glyphicon-exclamation-sign]
      (str " " (first e))]]))

(defn user-menu []
  (let [username "tim"]
    (when username
      [:li.dropdown
       [:a.dropdown-toggle {:href "#"
                            :data-toggle "dropdown"}
        [:kbd
         [:span.glyphicon.glyphicon-user]
         (str " " username)
         [:span.caret]]]
       [:ul.dropdown-menu {:role "menu"}
        [:li [:a {:href "#"} "preferences"]]
        [:li [:a {:href "#"} "logout"]]]])))

(defn header []
  [:header
   [:nav.navbar.navbar-inverse {:role "navigation"}
    [:div.container-fluid
     [:div.navbar-header
      [:button.navbar-toggle.collapsed
       {:type "button"
        :data-toggle "collapse"
        :data-target "#navbar-collapse"}
       [:span.sr-only "Toggle navigation"]
       [:span.icon-bar]
       [:span.icon-bar]
       [:span.icon-bar]]
      [:a.navbar-brand {:href "#/"}
       [:panel
        [:img {:src "img/brand.png"
               :height "40px"}]
        "  Leaderboard"
        [:span {:style {:font-family "cursive"}} "X"]]]]
     [:div.collapse.navbar-collapse {:id "navbar-collapse"}
      (into
       [:ul.nav.navbar-nav.navbar-right]
       (concat
        [[notifications]]
        (for [[k v] (sort-by key routes/view)]
          [:li [:a {:href (str "#/" (name k))} [:kbd (name k)]]])
        #_[[user-menu]]))]]]])
