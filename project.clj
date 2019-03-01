(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://leaderboardx.herokuapp.com"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.516" :scope "provided"]
                 [aysylu/loom "1.0.2"]
                 [bidi "2.1.5"]
                 [cljsjs/bootstrap "3.3.6-1"]
                 [cljsjs/d3 "5.7.0-0"]
                 [cljsjs/firebase "5.7.3-1"]
                 [cljsjs/google-platformjs-extern "1.0.0-0"]
                 [com.taoensso/encore "2.105.0"]
                 [compojure "1.6.1"]
                 [datascript "0.17.1"]
                 [devcards "0.2.6"]
                 [environ "1.1.0"]
                 [http-kit "2.3.0"]
                 [instaparse "1.4.10"]
                 [posh "0.5.6"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [reanimated "0.6.1"]
                 [ring "1.7.1"]
                 [task "a.2"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.5"]]

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.server.main

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:compiler {:output-to "resources/public/js/compiled/app.js"
                                        :externs ["externs.js"]}}}}

  :profiles
  {:dev
   {:env {:dev? true}
    :plugins [[lein-figwheel "0.5.18"]]
    :figwheel {:http-server-root "public"
               :css-dirs ["resources/public/css"]}
    :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src"]
                               :figwheel {:websocket-host "localhost"
                                          :on-jsload "algopop.leaderboardx.app.main/mount-root"}
                               :compiler {:main "dev.main"
                                          :optimizations :none
                                          :output-dir "resources/public/js/compiled/out"
                                          :asset-path "js/compiled/out"
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
                           ;; If you want source maps, source must be available
                           ;; :output-dir "resources/public/js/compiled/out"
                           ;; :source-map "resources/public/js/compiled/app.js.map"
                           :pretty-print false}}}}}})
