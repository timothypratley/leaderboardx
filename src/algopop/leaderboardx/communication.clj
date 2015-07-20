(ns algopop.leaderboardx.communication
  (:require [clojure.tools.logging :as log]
            [timothypratley.patchin :as patchin]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]))

(comment
(defonce router (atom nil))
(defonce app-states (atom {}))
(def model (atom {}))

(defn with-patch [app-states uid p]
  (-> app-states
      (update-in [uid :viewpoint] patchin/patch p)
      (assoc-in [uid :model] @model)))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn :client-id})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defn update-models []
  (doseq [uid (:any @connected-uids)
          :let [a (get-in @app-states [uid :model])
                b @model]
          :when (not= a b)
          :let [p (patchin/diff a b)]]
    (swap! app-states update-in [uid] assoc :model b)
    (chsk-send! uid [:edge/patch p])))

(defmulti event-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (log/info "Event:" event)
  (event-msg-handler ev-msg))

(defn stop-router! []
  (when-let [stop-f @router]
    (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(start-router!)

;; Message Handlers

(defmethod event-msg-handler :default
  [{:keys [event ?reply-fn]}]
  (log/info "Unhandled event:" event)
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

(defmethod event-msg-handler :chsk/uidport-open
  [{:keys [ring-req]}]
  (let [uid (get-in ring-req [:session :uid])]
    (log/info "New connection:" uid)
    (update-models)))

(defmethod event-msg-handler :chsk/uidport-close
  [{:keys [ring-req]}]
  (when-let [uid (get-in ring-req [:session :uid])]
    (swap! app-states dissoc uid)))

(defmethod event-msg-handler :chsk/ws-ping
  [_]
  ;; TODO: timeout old connections? Does sente already do this?
  )

(defmethod event-msg-handler :patchin/patch
  [{:keys [ring-req ?data]}]
  (when-let [uid (get-in ring-req [:session :uid])]
    (swap! app-states with-patch uid ?data)))
 )
