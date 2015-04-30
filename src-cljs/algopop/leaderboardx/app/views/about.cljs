(ns algopop.leaderboardx.app.views.about)

(defn about-page []
  [:div
   [:div.jumbotron
    [:h2 "Welcome to Leaderboard" [:span {:style {:font-family "cursive"}} "X"]]
    [:p "How do you rank?"]]
   [:div
    [:p "Athletes know exactly where they rank. Your employees will too."]]])
