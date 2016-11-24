(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://timothypratley.blogspot.com"

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [com.datomic/datomic-free "0.9.5407" :exclusions [joda-time]]
                 [com.lucasbradstreet/instaparse-cljs "1.4.1.2"]
                 [com.taoensso/sente "1.11.0"]
                 [compojure "1.5.1"]
                 [cljsjs/d3 "4.3.0-2"]
                 [cljsjs/react "15.4.0-0"]
                 [datascript "0.15.5"]
                 [environ "1.1.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-environ "1.1.0"]
            [lein-asset-minifier "0.3.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.main

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

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
    :plugins [[lein-figwheel "0.5.0-6"]]
    :figwheel {:http-server-root "public"
               :css-dirs ["resources/public/css"]
               :ring-handler algopop.leaderboardx.routes/handler}
    :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src"]
                               :figwheel {:websocket-host "localhost"
                                          :on-jsload "algopop.leaderboardx.app.main/mount-root"}
                               :compiler {:main "dev.main"
                                          :optimizations :none
                                          :source-map true
                                          :pretty-print true}}}}}

   :uberjar
   {:env {:production true}
    :source-paths ["env/prod/cljs" "src"]
    :hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
    :aot :all
    :omit-source true
    :cljsbuild {:jar true
                :builds {:app
                         {:compiler
                          {:main prod.main
                           :optimizations :advanced
                           :source-map "resources/public/js/compiled.main.map"
                           :pretty-print false}}}}}})
