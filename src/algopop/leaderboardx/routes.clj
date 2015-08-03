(ns algopop.leaderboardx.routes
  (:require [algopop.leaderboardx.pages :as pages]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :refer [not-found resources]]
            [environ.core :refer [env]]
            [prone.middleware :refer [wrap-exceptions]]
            [reloaded.repl :refer [system]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn unique-uid [user-id]
  (or
   (first (drop-while (:any @(:connected-uids (:sente system)))
                      (take 100 (iterate #(str % (rand-int 100))
                                         (if (seq user-id)
                                           user-id
                                           "guest")))))
   user-id))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    (println "Login request:" params)
    {:status 200 :session (assoc session :uid (unique-uid user-id))}))

(def site-routes
  (routes
    (GET "/" [] (pages/home (env :dev?)))
    (GET  "/chsk" req ((:ring-ajax-get-or-ws-handshake (:sente system)) req))
    (POST "/chsk" req ((:ring-ajax-post (:sente system)) req))
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
