(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX is a ranking product"
  :url "http://timothypratley.blogspot.com"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196" :scope "provided"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/sente "1.4.1"]
                 [compojure "1.3.3"]
                 [cljsjs/d3 "3.5.5-3"]
                 [cljsjs/react "0.13.1-0"]
                 [environ "1.0.0"]
                 [http-kit "2.1.19"]
                 [hiccup "1.0.5"]
                 [reagent "0.5.0"]
                 [reagent-forms "0.5.0"]
                 [reagent-utils "0.1.4"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.4"]
                 [prone "0.8.1"]
                 [secretary "1.2.3"]
                 [timothypratley/patchin "0.3.5"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-environ "1.0.0"]
            [lein-asset-minifier "0.2.2"]]

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
                                        :optimizations :none
                                        :pretty-print true}}}}

  :profiles {:dev {:repl-options {:init-ns repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.2"]
                                  [leiningen "2.5.1"]
                                  [figwheel "0.2.5"]
                                  [weasel "0.6.0"]
                                  [com.cemerick/piggieback "0.2.0"]
                                  [org.clojure/tools.nrepl "0.2.10"]]

                   :source-paths ["env/dev/clj" "src"]
                   :plugins [[lein-figwheel "0.2.5"]]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler algopop.leaderboardx.routes/handler}

                   :env {:dev? true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs" "src-cljs"]
                                              :compiler {:main "algopop.leaderboardx.dev"
                                                         :source-map true}}}}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs" "src-cljs"]
                                              :compiler
                                              ;; TODO: use advanced
                                              {:optimizations :whitespace
                                               :pretty-print false}}}}}})
