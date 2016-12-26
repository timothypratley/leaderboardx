(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://timothypratley.blogspot.com"

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [bidi "2.0.14"]
                 [cljsjs/d3 "4.3.0-2"]
                 [compojure "1.5.1"]
                 [com.lucasbradstreet/instaparse-cljs "1.4.1.2"]
                 [com.taoensso/encore "2.87.0"]
                 [datascript "0.15.5"]
                 [devcards "0.2.2"]
                 [posh "0.5.4"]
                 [reagent "0.6.0"]
                 [reagent-utils "0.2.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-asset-minifier "0.3.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.main

  :clean-targets ^{:protect false} ["resources/public/js/out" "resources/public/js/app.js"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:compiler {:output-to "resources/public/js/app.js"
                                        :output-dir "resources/public/js/out"
                                        :asset-path "js/out"
                                        :externs ["externs.js"]}}}}

  :profiles
  {:dev
   {:env {:dev? true}
    :plugins [[lein-figwheel "0.5.8"]]
    :figwheel {:http-server-root "public"
               :css-dirs ["resources/public/css"]}
    :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src"]
                               :figwheel {:websocket-host "localhost"
                                          :on-jsload "algopop.leaderboardx.app.main/mount-root"}
                               :compiler {:main "dev.main"
                                          :optimizations :none
                                          :source-map true
                                          :pretty-print true}}}}}

   :uberjar
   {:env {:production true}
    :hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
    :aot :all
    :omit-source true
    :cljsbuild {:builds {:app
                         {:jar true
                          :source-paths ["env/prod/cljs" "src"]
                          :compiler
                          {:main prod.main
                           :optimizations :advanced
                           :source-map "resources/public/js/compiled.main.map"
                           :pretty-print false}}}}}})
