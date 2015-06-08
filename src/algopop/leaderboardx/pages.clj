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
    [:script "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)})(window,document,'script','//www.google-analytics.com/analytics.js','ga');ga('create', 'UA-40336415-3', 'auto');ga('send', 'pageview');"]
    [:script "var $buoop = {c:2}; function $buo_f(){var e = document.createElement(\"script\"); e.src = \"//browser-update.org/update.min.js\"; document.body.appendChild(e);}; try {document.addEventListener(\"DOMContentLoaded\", $buo_f,false)} catch(e){window.attachEvent(\"onload\", $buo_f)}"]]
   [:body
    [:div {:id "app"}
     "<div class='container'><header><nav role='navigation' class='navbar navbar-inverse'><div class='container-fluid'><div class='navbar-header'><button type='button' data-toggle='collapse' data-target='#navbar-collapse' class='navbar-toggle collapsed'><span class='sr-only'>Toggle navigation</span><span class='icon-bar'></span><span class='icon-bar'></span><span class='icon-bar'></span></button><a href='#/' class='navbar-brand'><panel><img src='img/brand.png' height='40px'><span>  Leaderboard</span><span style='font-family:cursive;'>X</span></panel></a></div><div id='navbar-collapse' class='collapse navbar-collapse'><ul class='nav navbar-nav navbar-right'><noscript><a href='http://enable-javascript.com/'><kbd>Please enable JavaScript</kbd></a></noscript></ul></div></div></nav></header><div class='well'><div><div class='jumbotron'><h2><span>Welcome to Leaderboard</span><span style='font-family:cursive;'>X</span></h2><p>How do you rank?</p></div><div><p>Contact timothypratley@gmail.com</p></div></div></div></div>"]
    [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"}]
    [:script {:src "//maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js"}]
    [:script {:src "js/app.js" :type "text/javascript"}]]))
