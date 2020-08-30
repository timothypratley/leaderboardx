(ns algopop.leaderboardx.app.firebase
  (:require
    [algopop.leaderboardx.app.firebase-serialization :as s]
    [cljsjs.firebase]
    [clojure.string :as str]
    [reagent.core :as reagent]))

(defonce user
  (reagent/atom nil))

(def timestamp
  ;; TODO: test advanced compilation
  js/firebase.database.ServerValue.TIMESTAMP)

(defn uid []
  (:uid @user))

(defn is-user-equal [google-user firebase-user]
  (and
    firebase-user
    (some
      #(and (= (.-providerId %) js/firebase.auth.GoogleAuthProvider.PROVIDER_ID)
            (= (.-uid %) (.getId (.getBasicProfile google-user))))
      (:provider-data firebase-user))))

(defn db-path [path]
  (str/join "/" (map s/escape path)))

(defn db-ref
  ([]
   (.ref (js/firebase.database)))
  ([path]
   (.ref (js/firebase.database) (db-path path))))

(defn user-entities-path []
  ["users" (uid) "entities"])

(defn user-entities []
  (db-ref (user-entities-path)))

(defn ref-set [r value]
  (.set r (s/clj->firebase value)))

(defn db-set [path value]
  (ref-set (db-ref path) value))

(defn ref-update [[r & path] value]
  (-> r
      (cond-> (seq path) (.child (db-path path)))
      (.update (s/clj->firebase value))))

(defn db-update [path value]
  (ref-update (db-ref path) value))

(defn ^:export onSignIn [google-user]
  (when (not (is-user-equal google-user @user))
    (.catch
      (.signInWithCredential
        (js/firebase.auth)
        (js/firebase.auth.GoogleAuthProvider.credential
          (.-id_token (.getAuthResponse google-user))))
      (fn [error]
        (js/alert error)))))

(defn on-auth []
  (.onAuthStateChanged
    (js/firebase.auth)
    (fn auth-state-changed [firebase-user]
      (when firebase-user
        (let [uid (.-uid firebase-user)
              display-name (.-displayName firebase-user)
              photo-url (.-photoURL firebase-user)
              provider-data (.-providerData firebase-user)]
          (if uid
            (do
              (db-set ["users" uid "settings"]
                      {:photo-url photo-url
                       :display-name display-name})
              (reset! user {:photo-url photo-url
                            :display-name display-name
                            :uid uid
                            :provider-data provider-data}))
            (when @user
              (reset! user nil))))))
    (fn auth-error [error]
      (js/alert error))))

(defonce app (reagent/atom nil))

(defn init []
  (when-not @app
    (reset!
      app
      (js/firebase.initializeApp
        #js {:apiKey "AIzaSyDXxQlA3tXPpoNA1COmHlLdPkkQ2DyK5jI"
             :authDomain "leaderboardx-f380d.firebaseapp.com"
             :databaseURL "https://leaderboardx-f380d.firebaseio.com"
             :storageBucket "leaderboardx-f380d.appspot.com"}))
    (on-auth)))

(defn sign-in []
  ;; TODO: use Credential for mobile.
  (.signInWithRedirect
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(defn sign-out []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  (reset! user nil))


(defn on
  "Takes a path and a component.
  Component takes a ratom as it's first argument, and optional other arguments.
  The atom derefs to the firebase state at path."
  [path component query]
  (reagent/with-let
    [r (db-ref path)
     a (reagent/atom nil)]
    (-> r
        (cond-> query query)
        (.on "value" (fn [x]
                       (reset! a (s/firebase->clj (.val x))))))
    [component a r]
    (finally
      (.off r))))

(defn in [ratom path query]
  (reagent/with-let
    [r (db-ref path)]
    (-> r
        (cond-> query query)
        ;; should be child?
        (.on "value" (fn [x]
                       (reset! ratom (s/firebase->clj (.val x))))))
    (finally (.off r))))

(defn watch-graph [a path]
  (reagent/with-let [graph-ref (db-ref path)
                     nodes-ref (db-ref (conj path "nodes"))
                     edges-ref (db-ref (conj path "edges"))
                     entity-refs (atom {})]
    (.on graph-ref "value"
         (fn [x]
           (reset! a (s/firebase->clj (.val x)))))
    (-> nodes-ref
        (.on "child_added"
             (fn [x]
               (let [k (.-key x)
                     v (.val x)]
                 (let [r (db-ref (conj user-entities-path k))]
                   (.on r "value"
                        (fn [z]
                          ;;TODO
                          ))
                   (swap! entity-refs assoc k r)))
               ))
        (.on "child_changed"
             (fn []
               ))
        (.on "child_removed"
             (fn [x]
               (let [k (.-key x)]
                 (.off (get @entity-refs k))
                 (swap! entity-refs dissoc k)))))
    (-> edges-ref
        (.on "child_added"
             (fn []
               ))
        (.on "child_changed"
             (fn []
               ))
        (.on "child_removed"
             (fn []
               ;; TODO
               #_(.off))))
    nil
    (finally
      (.off @graph-ref)
      (.off @nodes-ref)
      (.off @edges-ref)
      (doseq [[k v] @entity-refs]
        (.off v)))))

(defn aeon
  "Given an atom a,
  will update that atom from querying path (which may change)."
  [a path query]
  (reagent/with-let [r (reagent/atom nil)]
    #_(when @a
       (reset! a nil))
    #_(when @r
       (.off @r))
    (reset!
      r
      (doto (db-ref path)
        (cond-> query query)
        (.on "value"
             (fn [x]
               (reset! a (s/firebase->clj (.val x)))))))
    nil
    (finally
      (.off @r))))

(defn subaeon [a path]
  (reagent/with-let [r (reagent/atom nil)]
    (prn "subaeon" path)
    (when @r
      (prn "WARNING: this shouldn't happen, is the set keyed?")
      (swap! a dissoc (last path))
      (.off @r))
    (reset!
      r
      (doto (db-ref path)
        (.on "value"
             (fn [x]
               (swap! a assoc (last path) (s/firebase->clj (.val x)))))))
    nil
    (finally
      (swap! a dissoc (last path))
      (.off @r))))

(defn ref-push
  ([r]
   (.push r))
  ([r value]
   (.push r (s/clj->firebase value))))

(defn db-push
  ([path] (ref-push (db-ref path)))
  ([path value] (ref-push (db-ref path) value)))

(defn ref-remove [[r & path]]
  (-> r
      (cond-> (seq path) (.child (db-path path)))
      (.remove)))

(defn db-remove [path]
  (ref-remove (db-ref path)))

(comment
  ;; isolate
  entities (reagent/atom {})
  nodes (reaction
          (doto
            (for [[k v] @entities
                  :let [v (set/rename-keys
                            (walk/keywordize-keys v)
                            {:edge-type :edge/type})]
                  :when (not (:edge/type v))]
              (assoc v :db/id k
                       :node/name k))))
  edges (reaction
          (for [[k v] @entities
                :let [v (set/rename-keys
                          (walk/keywordize-keys v)
                          {:edge-type :edge/type
                           :from :edge/from
                           :to :edge/to})]
                :when (:edge/type v)]
            (assoc v :db/id k
                     :edge/name k)))
  ;; TODO: downstream users want to modify
  g (reaction
      {:nodes @nodes
       :edges @edges})
  )
