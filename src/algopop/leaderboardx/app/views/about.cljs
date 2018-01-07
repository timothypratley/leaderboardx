(ns algopop.leaderboardx.app.views.about)

(defn about-page []
  [:div
   [:div.jumbotron
    [:h2 "Welcome to Leaderboard" [:i "X"]]
    [:p "This is a tool for quickly building graphs representions of relationships."]
    [:small "Contact timothypratley@gmail.com"]]
   [:div.well
    [:p "Click `graph-editor` in the top right navigation menu to get started."]
    [:p "There is a help button in the graph-editor with usage instructions."]]
   [:div
    [:div.embed-responsive.embed-responsive-16by9
     [:iframe.embed-responsive-item
      {:src "//www.youtube.com/embed/RMTFP43Ce98"
       :allow-full-screen "true"}]]
    #_[:p [:a {:href "//algopop.herokuapp.com"
             :target "_blank"}
         "Created by Algopop"]]]])

(defn forum-page []
  [:div.well
   [:div#disqus_thread
    {:ref
     (fn [this]
       (when (and this js/window.DISQUS)
         (js/window.DISQUS.reset #js {:reload true})))}]])
