(ns ^:figwheel-no-load dev.main
  (:require [algopop.leaderboardx.app.main :as main]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback main/mount-root)

(main/init!)
