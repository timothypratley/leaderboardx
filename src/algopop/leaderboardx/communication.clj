(ns algopop.leaderboardx.communication
  (:require [algopop.leaderboardx.db :as db]
            [timothypratley.patchin :as patchin]
            [taoensso.sente :as sente]
            [reloaded.repl :refer [system]]))

(defonce router (atom nil))
(defonce app-states (atom {}))

(defn with-patch [app-states uid p]
  (-> app-states
      (update-in [uid :viewpoint] patchin/patch p)
      (assoc-in [uid :model] (db/pull-artist "John Lennon"))))

(defn update-models []
  (doseq [uid (:any @(:connected-uids (:sente system)))
          :let [a (get-in @app-states [uid :model])
                b (db/pull-artist "John Lennon")]
          :when (not= a b)
          :let [p (patchin/diff a b)]]
    (swap! app-states update-in [uid] assoc :model b)
    ((:chsk-send! (:sente system)) uid [:edge/patch p])))

(defmulti msg :id)

(defn event-msg-handler [{:keys [event] :as ev-msg}]
  (println "Event:" event)
  (msg ev-msg))

;; Message Handlers

(defmethod msg :default [{:keys [event ?reply-fn]}]
  (println "Unhandled event:" event)
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

(defmethod msg :chsk/uidport-open [{:keys [ring-req]}]
  (let [uid (get-in ring-req [:session :uid])]
    (println "New connection:" uid)
    (update-models)))

(defmethod msg :chsk/uidport-close [{:keys [ring-req]}]
  (when-let [uid (get-in ring-req [:session :uid])]
    (swap! app-states dissoc uid)))

(defmethod msg :chsk/ws-ping [_]
  ;; TODO: timeout old connections? Does sente already do this?
  )

(defmethod msg :patchin/patch [{:keys [ring-req ?data]}]
  (when-let [uid (get-in ring-req [:session :uid])]
    (swap! app-states with-patch uid ?data)))
