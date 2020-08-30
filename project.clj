(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://leaderboardx.herokuapp.com"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; warning: later versions of cljs break looms remove-nodes, which breaks leaderboardx rename
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [aysylu/loom "1.0.2"]
                 [bidi "2.1.6"]
                 [cljsjs/bootstrap "3.3.6-1"]
                 [cljsjs/d3 "5.12.0-0"]
                 [cljsjs/firebase "7.5.0-0"]
                 [cljsjs/google-platformjs-extern "1.0.0-0"]
                 [com.taoensso/encore "2.119.0"]
                 [compojure "1.6.1"]
                 [datascript "0.18.8"]
                 [devcards "0.2.6"]
                 [environ "1.1.0"]
                 [http-kit "2.3.0"]
                 [instaparse "1.4.10"]
                 [justice "0.0.4-alpha"]
                 [meander/epsilon "0.0.378"]
                 [reagent "0.9.1"]
                 [reagent-utils "0.3.3"]
                 [reanimated "0.6.1"]
                 [ring "1.8.0"]
                 [task "a.2"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.6"]]

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.server.main

  :clean-targets ^{:protect false} ["resources/public/js/compiled" :target-path]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:compiler {:output-to "resources/public/js/compiled/app.js"
                                        :externs ["externs.js"]}}}}

  :profiles
  {:dev
   {:env {:dev? true}
    :plugins [[lein-figwheel "0.5.19"]]
    :figwheel {:http-server-root "public"
               :css-dirs ["resources/public/css"]}
    :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src"]
                               :figwheel {:on-jsload "algopop.leaderboardx.app.main/mount-root"}
                               :compiler {:main dev.main
                                          :optimizations :none
                                          :output-dir "resources/public/js/compiled/out"
                                          :asset-path "js/compiled/out"
                                          :source-map true
                                          :source-map-timestamp true
                                          :pretty-print true}}
                         :dev {:source-paths ["env/dev/cljs" "src" "test"]
                               :figwheel {:devcards true}
                               :compiler {:main dev.devcards
                                          :optimizations :none
                                          :output-to "resources/public/js/compiled/devcards.js"
                                          :output-dir "resources/public/js/compiled/devcards"
                                          :asset-path "js/compiled/devcards"
                                          :source-map true
                                          :source-map-timestamp true
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
