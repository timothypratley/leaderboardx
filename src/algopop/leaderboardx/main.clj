(ns algopop.leaderboardx.main
  (:require [algopop.leaderboardx.communication :as comm]
            [algopop.leaderboardx.routes :as routes]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit])
  (:gen-class))

(defn start-http-kit [ring-handler port]
  (println "Starting http-kit...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:port (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(defonce web-server (atom nil))

(defn stop-web-server! []
  (when-let [m @web-server]
    ((:stop-fn m))))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [port] :as server-map}
        (start-http-kit (var routes/handler)
                        (or port 0))
        uri (format "http://localhost:%s/" port)]
    (log/info "Web server is running at" uri)
    (reset! web-server server-map)))

(defn start! [port]
  (comm/start-router!)
  (start-web-server! port))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (start! port)))
