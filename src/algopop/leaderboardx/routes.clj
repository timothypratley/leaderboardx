(ns algopop.leaderboardx.routes
  (:require [algopop.leaderboardx.pages :as pages]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :refer [not-found]]
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

(defn status []
  (prn "SYS" (into {} system))
  {:status 200})

(def site-routes
  (routes
    (GET "/" [] (pages/home (env :dev?)))
    (GET  "/chsk" req ((:ring-ajax-get-or-ws-handshake (:sente system)) req))
    (POST "/chsk" req ((:ring-ajax-post (:sente system)) req))
    (GET "/login" req (login! req))
    (POST "/login" req (login! req))
    (GET "/status" req (status))
    (not-found "Not Found")))

(def defaults
  (-> site-defaults
      (assoc-in [:security :anti-forgery]
                {:read-token (fn [req] (-> req :params :csrf-token))})
      #_(assoc-in [:static :resources] "public")))

(def handler
  (cond-> (wrap-defaults site-routes defaults)
    (env :dev?) (-> wrap-exceptions wrap-reload)))
