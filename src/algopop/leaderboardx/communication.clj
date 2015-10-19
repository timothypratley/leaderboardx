(ns algopop.leaderboardx.communication
  (:require
   [algopop.leaderboardx.db :as db]
   [timothypratley.patchin :as patchin]
   [taoensso.sente :as sente]
   [reloaded.repl :refer [system]]))

(defonce router (atom nil))
(defonce app-states (atom {}))

(defn commands [{{{:keys [commands]} :route} :viewpoint :as user-state}]
  (prn "COMMANDS" user-state)
  (doseq [[date command] commands]
    (prn "COMMAND" command)
    (db/transact command))
  user-state)

(defn update-uid [user-state p]
  (-> user-state
      (update :viewpoint patchin/patch p)
      (commands)
      (assoc :model {} #_(db/pull-artist "John Lennon"))))

(defn with-patch [app-states uid p]
  (update app-states uid update-uid p))

(defn model [{{:keys [page]} :route :as viewpoint}]
  (prn "VIEWPOINT" viewpoint)
   (if (= page "assess")
     (db/pull :assessment-template/name "player-assessment")
     (db/pull :assessee/name "Tim")))

(defn update-models []
  (doseq [uid (:any @(:connected-uids (:sente system)))
          :let [a (get-in @app-states [uid :model])
                b (model (get-in @app-states [uid :viewpoint]))]
          :when (not= a b)
          :let [p (patchin/diff a b)]]
    (swap! app-states update-in [uid] assoc :model b)
    ((:chsk-send! (:sente system)) uid [:edge/patch p])))

(defmulti msg :id)

(defn event-msg-handler [{:keys [event] :as ev-msg}]
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
  (prn "RECEIVED PATCH" (get-in ring-req [:session :uid]))
  (when-let [uid (get-in ring-req [:session :uid])]
    (swap! app-states with-patch uid ?data)))
