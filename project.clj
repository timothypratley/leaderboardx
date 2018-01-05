(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://timothypratley.blogspot.com"

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.671" :scope "provided"]
                 [bidi "2.1.1"]
                 [cljsjs/bootstrap "3.3.6-1"]
                 [cljsjs/d3 "4.3.0-5"]
                 [cljsjs/firebase "4.0.0-0"]
                 [cljsjs/google-platformjs-extern "1.0.0-0"]
                 [compojure "1.6.0"]
                 ;; TODO: don't need it
                 [com.lucasbradstreet/instaparse-cljs "1.4.1.2"]
                 [com.taoensso/encore "2.91.1"]
                 [datascript "0.16.1"]
                 [devcards "0.2.3"]
                 [posh "0.5.6"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [recalcitrant "0.1.1"]
                 [binaryage/devtools "0.9.8"]]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-asset-minifier "0.3.2"]]

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
    :plugins [[lein-figwheel "0.5.11"]]
    :figwheel {:http-server-root "public"
               :css-dirs ["resources/public/css"]}
    :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src"]
                               :figwheel {:websocket-host "localhost"
                                          :on-jsload "algopop.leaderboardx.app.main/mount-root"}
                               :compiler {:main "dev.main"
                                          :optimizations :none
                                          :source-map true
                                          :pretty-print true
                                          :preloads [devtools.preload]}}}}}

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
