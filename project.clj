(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://leaderboardx.herokuapp.com"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [aysylu/loom "1.0.0"]
                 [bidi "2.1.2"]
                 [cljsjs/bootstrap "3.3.6-1"]
                 [cljsjs/d3 "4.12.0-0"]
                 [cljsjs/firebase "4.8.1-0"]
                 [cljsjs/google-platformjs-extern "1.0.0-0"]
                 [com.taoensso/encore "2.93.0"]
                 [compojure "1.6.0"]
                 [datascript "0.16.3"]
                 [devcards "0.2.4"]
                 [environ "1.1.0"]
                 [http-kit "2.2.0"]
                 [instaparse "1.4.8"]
                 [posh "0.5.6"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [reanimated "0.6.0"]
                 [recalcitrant "0.1.2"]
                 [ring "1.6.3"]
                 [task "a.2"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.4"]]

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.server.main

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:compiler {:output-to "resources/public/js/compiled/app.js"
                                        :output-dir "resources/public/js/compiled/out"
                                        :asset-path "js/compiled/out"
                                        :externs ["externs.js"]}}}}

  :profiles
  {:dev
   {:env {:dev? true}
    :plugins [[lein-figwheel "0.5.14"]]
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
    :hooks [leiningen.cljsbuild]
    :aot :all
    :omit-source true
    :cljsbuild {:builds {:app
                         {:jar true
                          :source-paths ["env/prod/cljs" "src"]
                          :compiler
                          {:main prod.main
                           :optimizations :advanced
                           :infer-externs true
                           :source-map "resources/public/js/compiled/app.js.map"
                           :pretty-print false}}}}}})
