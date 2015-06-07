(ns algopop.leaderboardx.pages
  (:require [hiccup.page :as page]))

(defn home [dev?]
  (page/html5
   [:head
    [:title "LeaderboardX"]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "apple-touch-icon" :sizes "57x57" :href "/apple-touch-icon-57x57.png"}]
    [:link {:rel "apple-touch-icon" :sizes "60x60" :href "/apple-touch-icon-60x60.png"}]
    [:link {:rel "apple-touch-icon" :sizes "72x72" :href "/apple-touch-icon-72x72.png"}]
    [:link {:rel "apple-touch-icon" :sizes "76x76" :href "/apple-touch-icon-76x76.png"}]
    [:link {:rel "apple-touch-icon" :sizes "114x114" :href "/apple-touch-icon-114x114.png"}]
    [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/apple-touch-icon-120x120.png"}]
    [:link {:rel "apple-touch-icon" :sizes "144x144" :href "/apple-touch-icon-144x144.png"}]
    [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/apple-touch-icon-152x152.png"}]
    [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/apple-touch-icon-180x180.png"}]
    [:link {:rel "icon" :type "image/png" :href "/favicon-32x32.png" :sizes "32x32"}]
    [:link {:rel "icon" :type "image/png" :href "/android-chrome-192x192.png" :sizes "192x192"}]
    [:link {:rel "icon" :type "image/png" :href "/favicon-96x96.png" :sizes "96x96"}]
    [:link {:rel "icon" :type "image/png" :href "/favicon-16x16.png" :sizes "16x16"}]
    [:link {:rel "manifest" :href "/manifest.json"}]
    [:meta {:name "msapplication-TileColor" :content "#da532c"}]
    [:meta {:name "msapplication-TileImage" :content "/mstile-144x144.png"}]
    [:meta {:name "theme-color" :content "#ffffff"}]
    [:link {:rel "stylesheet" :href "css/site.css"}]
    [:link {:rel "stylesheet" :href "//netdna.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css"}]
    [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"}]
    [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js"}]
    [:script "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)})(window,document,'script','//www.google-analytics.com/analytics.js','ga');ga('create', 'UA-40336415-3', 'auto');ga('send', 'pageview');"]]
   [:body
    [:div {:id "app"}
     "<div class='container' data-reactid='.0'><header data-reactid='.0.0'><nav role='navigation' class='navbar navbar-inverse' data-reactid='.0.0.0'><div class='container-fluid' data-reactid='.0.0.0.0'><div class='navbar-header' data-reactid='.0.0.0.0.0'><button type='button' data-toggle='collapse' data-target='#navbar-collapse' class='navbar-toggle collapsed' data-reactid='.0.0.0.0.0.0'><span class='sr-only' data-reactid='.0.0.0.0.0.0.0'>Toggle navigation</span><span class='icon-bar' data-reactid='.0.0.0.0.0.0.1'></span><span class='icon-bar' data-reactid='.0.0.0.0.0.0.2'></span><span class='icon-bar' data-reactid='.0.0.0.0.0.0.3'></span></button><a href='#/' class='navbar-brand' data-reactid='.0.0.0.0.0.1'><panel data-reactid='.0.0.0.0.0.1.0'><img src='img/brand.png' height='40px' data-reactid='.0.0.0.0.0.1.0.0'><span data-reactid='.0.0.0.0.0.1.0.1'>  Leaderboard</span><span style='font-family:cursive;' data-reactid='.0.0.0.0.0.1.0.2'>X</span></panel></a></div><div id='navbar-collapse' class='collapse navbar-collapse' data-reactid='.0.0.0.0.1'><ul class='nav navbar-nav navbar-right' data-reactid='.0.0.0.0.1.0'><noscript data-reactid='.0.0.0.0.1.0.0'></noscript><li data-reactid='.0.0.0.0.1.0.1'><a href='#/about' data-reactid='.0.0.0.0.1.0.1.0'><kbd data-reactid='.0.0.0.0.1.0.1.0.0'>about</kbd></a></li><li data-reactid='.0.0.0.0.1.0.2'><a href='#/endorse' data-reactid='.0.0.0.0.1.0.2.0'><kbd data-reactid='.0.0.0.0.1.0.2.0.0'>endorse</kbd></a></li><li data-reactid='.0.0.0.0.1.0.3'><a href='#/graph-editor' data-reactid='.0.0.0.0.1.0.3.0'><kbd data-reactid='.0.0.0.0.1.0.3.0.0'>graph-editor</kbd></a></li><li data-reactid='.0.0.0.0.1.0.4'><a href='#/home' data-reactid='.0.0.0.0.1.0.4.0'><kbd data-reactid='.0.0.0.0.1.0.4.0.0'>home</kbd></a></li><li class='dropdown' data-reactid='.0.0.0.0.1.0.5'><a href='#' data-toggle='dropdown' class='dropdown-toggle' data-reactid='.0.0.0.0.1.0.5.0'><kbd data-reactid='.0.0.0.0.1.0.5.0.0'><span class='glyphicon glyphicon-user' data-reactid='.0.0.0.0.1.0.5.0.0.0'></span><span data-reactid='.0.0.0.0.1.0.5.0.0.1'> tim</span><span class='caret' data-reactid='.0.0.0.0.1.0.5.0.0.2'></span></kbd></a><ul role='menu' class='dropdown-menu' data-reactid='.0.0.0.0.1.0.5.1'><li data-reactid='.0.0.0.0.1.0.5.1.0'><a href='#' data-reactid='.0.0.0.0.1.0.5.1.0.0'>preferences</a></li><li data-reactid='.0.0.0.0.1.0.5.1.1'><a href='#' data-reactid='.0.0.0.0.1.0.5.1.1.0'>logout</a></li></ul></li></ul></div></div></nav></header><div class='well' data-reactid='.0.1'><div data-reactid='.0.1.0'><div class='jumbotron' data-reactid='.0.1.0.0'><h2 data-reactid='.0.1.0.0.0'><span data-reactid='.0.1.0.0.0.0'>Welcome to Leaderboard</span><span style='font-family:cursive;' data-reactid='.0.1.0.0.0.1'>X</span></h2><p data-reactid='.0.1.0.0.1'>How do you rank?</p></div><div data-reactid='.0.1.0.1'><p data-reactid='.0.1.0.1.0'>Contact timothypratley@gmail.com</p><p data-reactid='.0.1.0.1.1'><a href='//algopop.herokuapp.com' target='_blank' data-reactid='.0.1.0.1.1.0'>Created by Algopop</a></p></div></div></div></div></div><script src='js/app.js' type='text/javascript'></script><input type='text' name='history_state0' id='history_state0' style='display:none'></body></html>"]
    [:script {:src "js/app.js" :type "text/javascript"}]]))
