(defproject algopop/leaderboardx "0.1.0-SNAPSHOT"
  :description "LeaderboardX"
  :url "http://timothypratley.blogspot.com"

  :aliases {"migrate" ["trampoline" "run" "-m"
                       "algopop.leaderboardx.db.schema/migrate"]}

  :repositories
  {"my.datomic.com"
   {:url "https://my.datomic.com/repo"
    :username [:gpg :env/datomic_username]
    :passphrase [:gpg :env/datomic_passphrase]
    :creds :gpg}}

  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [org.clojure/clojurescript "1.9.36"]
   ;;[com.datomic/datomic-free "0.9.5206" :exclusions [joda-time]]
   [com.cognitect/transit-clj "0.8.285" :exclusions [commons-codec]]
   [com.cognitect/transit-cljs "0.8.237"]
   [com.datomic/datomic-pro  "0.9.5206" :exclusions [joda-time]]
   [com.lucasbradstreet/instaparse-cljs "1.4.1.2"]
   [com.taoensso/sente "1.8.1"]
   [compojure "1.5.0"]
   [cljsjs/d3 "3.5.16-0"]
   [datascript "0.15.0"]
   [datomic-schema "1.3.0"]
   [devcards "0.2.1-7"]
   [environ "1.0.3"]
   [http-kit "2.1.19"]
   [hiccup "1.0.5"]
   [prone "1.1.1"]
   [reagent "0.5.1"]
   [reagent-forms "0.5.24"]
   [reagent-utils "0.1.8"]
   [reloaded.repl "0.2.2"]
   ;[org.clojure/tools.nrepl "0.2.12"]
   [cider/cider-nrepl "0.12.0"]
   [ring "1.5.0"]
   [ring/ring-defaults "0.2.1"]
   [bidi "2.0.9"]
   [timothypratley/patchin "0.3.5"]]

  :plugins
  [[lein-cljsbuild "1.1.3"]
   [lein-environ "1.0.3"]
   [lein-asset-minifier "0.3.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "algopop-leaderboardx-standalone.jar"

  :main algopop.leaderboardx.main

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild
  {:builds {:app {:compiler {:output-dir "resources/public/js/compiled"
                             :output-to "resources/public/js/compiled/main.js"
                             :asset-path "js/compiled"}}}}

  :profiles
  {:dev {:env {:dev? "true"}
         :plugins [[lein-figwheel "0.5.4"]
                   [lein-environ "1.0.3"]]
         :figwheel {:http-server-root "public"
                    :server-port 3449
                    :nrepl-port 7888
                    :css-dirs ["resources/public/css"]}
         :cljsbuild
         {:builds
          {:app {:figwheel {:websocket-host "localhost"
                            ;; TODO: move to code?
                            ;; or remove reload code
                            :on-jsload "algopop.leaderboardx.app.main/mount-root"}
                 :source-paths ["src" "env/dev/cljs"]
                 :compiler {:main dev.main
                            :optimizations :none
                            :source-map true
                            :pretty-print true}}
           :devcards {:figwheel {:devcards true}
                      :source-paths ["src" "env/dev/cljs"]
                      :compiler {:main dev.devcards
                                 :optimizations :none
                                 :output-to "resources/public/js/compiled/devcards.js"
                                 :output-dir "resources/public/js/devcards"
                                 :asset-path "js/devcards"
                                 :source-map-timestamp true}}}}}

   :uberjar
   {:env {:production true}
    :source-paths ["env/prod/cljs" "src-cljs"]
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
