(ns algopop.leaderboardx.app.views.header
  (:require [reagent.session :as session]))

(defn header []
  [:header
   [:nav.navbar.navbar-inverse {:role "navigation"
                               }
    [:div.container-fluid
     [:div.navbar-header
      [:button.navbar-toggle {:type "button"
                              :data-toggle "collapse"
                              :data-target "navbar-collapse"}
       [:span.sr-only "Toggle navigation"]
       [:span.icon-bar]
       [:span.icon-bar]
       [:span.icon-bar]]
      [:a.navbar-brand {:href "#"}
       [:panel
        [:img {:src "img/brand.png"
                      :height "40px"}]
        "  Leaderboard"
        [:span {:style {:font-family "cursive"}} "X"]]]]
     [:div.collapse.navbar-collapse {:id "navbar-collapse"}
      [:ul.nav.navbar-nav.navbar-right
       (when-not (session/get :open?)
         [:li
          [:p.navbar-text
           [:span.glyphicon.glyphicon-exclamation-sign]
           " Disconnected from server."]])
       [:li [:a {:href "#/graph"} [:kbd "graph"]]]
       [:li [:a {:href "#/about"} [:kbd "about"]]]
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
             [:li [:a {:href "#"} "logout"]]]]))]]]]])
