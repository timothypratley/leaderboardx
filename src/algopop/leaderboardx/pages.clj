(ns algopop.leaderboardx.pages
  (:require [hiccup.page :as page]))

(defn home [dev?]
  (page/html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible"
            :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "shortcut icon" :href "favicon.ico" :type "image/x-icon"}]
    [:link {:rel "stylesheet"
            :href "css/site.css"}]
    [:link {:rel "stylesheet"
            :href "//netdna.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css"}]
    [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"}]
    [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js"}]]

   [:body
    [:div {:id "app"}]
    [:script {:src "js/app.js"
              :type "text/javascript"}]]))
