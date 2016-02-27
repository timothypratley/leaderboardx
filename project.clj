(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX is a ranking product"
  :url "http://timothypratley.blogspot.com"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.lucasbradstreet/instaparse-cljs "1.4.1.0"]
                 [com.taoensso/sente "1.6.0"]
                 [compojure "1.4.0"]
                 [cljsjs/d3 "3.5.7-1"]
                 [cljsjs/react "0.14.3-0"]
                 [datascript "0.15.0"]
                 [environ "1.0.2"]
                 [http-kit "2.1.19"]
                 [hiccup "1.0.5"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.16"]
                 [reagent-utils "0.1.7"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [prone "1.0.2"]
                 [secretary "1.2.3"]
                 [timothypratley/patchin "0.3.5"]]

  :plugins [[lein-cljsbuild "1.1.2" :exclusions [org.clojure/clojure]]
            [lein-environ "1.0.2"]
            [lein-asset-minifier "0.2.7" :exclusions [org.clojure/clojure]]]

  :ring {:handler algopop.leaderboardx/handler
         :uberwar-name "algopop.leaderboardx.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.main

  :clean-targets ^{:protect false} ["resources/public/js"]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:compiler {:output-to "resources/public/js/app.js"
                                        :output-dir "resources/public/js/out"
                                        :asset-path "js/out"
                                        :externs ["externs.js"]}}}}

  :profiles {:dev {:plugins [[lein-figwheel "0.5.0-6"]]
                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler algopop.leaderboardx.routes/handler}
                   :env {:dev? true}
                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src-cljs"]
                                              :figwheel {:websocket-host "localhost"
                                                         :on-jsload "algopop.leaderboardx.app.main/mount-root"}
                                              :compiler {:main "dev.main"
                                                         :optimizations :none
                                                         :source-map true
                                                         :pretty-print true}}}}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs" "src-cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :source-map "resources/public/js/app.map"
                                               :pretty-print false}}}}}})
