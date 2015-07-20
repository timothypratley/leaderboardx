(ns algopop.leaderboardx.routes
  (:require [algopop.leaderboardx.pages :as pages]
            [algopop.leaderboardx.services :as services]
            ;;[algopop.leaderboardx.communication :as comm]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    (log/info "Login request:" params)
    {:status 200 :session (assoc session :uid user-id)}))

(def site-routes
  (routes
    (GET "/" [] (pages/home (env :dev?)))
    ;;(GET "/chsk" req (comm/ring-ajax-get-or-ws-handshake req))
    ;;(POST "/chsk" req (comm/ring-ajax-post req))
    (GET "/login" req (login! req))
    (POST "/login" req (login! req))
    (resources "/")
    (not-found "Not Found")))

(def ring-defaults-config
  (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
            {:read-token (fn [req] (-> req :params :csrf-token))}))

(def handler
  (cond-> (wrap-defaults site-routes ring-defaults-config)
    (env :dev?) (-> wrap-exceptions wrap-reload)))
