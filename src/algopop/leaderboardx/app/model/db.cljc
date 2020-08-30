(ns algopop.leaderboardx.app.model.db
  (:require [algopop.leaderboardx.app.model.seed :as seed]
            [clojure.set :as set]
            [clojure.string :as str]
            [datascript.core :as d]
            [datascript.impl.entity :as de]
            [justice.core :as j]
            [taoensso.encore :as encore]))

(def base-schema
  "The minimum schema required to build custom schemas on top of.
  Intentionally not inserted into the DB."
  (merge
   #:edge-type{:name #:db{:unique :db.unique/identity}
               :from-identity #:db{:valueType :db.type/ref}
               :to-identity #:db{:valueType :db.type/ref}
               :cardinality {}}
   #:edge{:name #:db{:unique :db.unique/identity}
          :type #:db{:valueType :db.type/ref}
          :from #:db{:valueType :db.type/ref}
          :to #:db{:valueType :db.type/ref}}))

(def ^:dynamic conn
  (doto (d/create-conn base-schema)
    (j/attach)))

(def default-schema
  "Defines some useful node patterns used by roster.
  Can specify types, these are stripped from the datascript schema,
  but stored so the UI can make use of them."
  (merge
   #:person{:name #:db{:unique :db.unique/identity}
            :height #:db{:valueType :db.type/double}}
   #:role{:name #:db{:unique :db.unique/identity}}
   #:weekday{:name #:db{:unique :db.unique/identity}}
   #:duty{:name #:db{:unique :db.unique/identity}
          :start #:db{:valueType :db.type/instant}
          :end #:db{:valueType :db.type/instant}}
   #:event{:name #:db{:unique :db.unique/identity}
           :start #:db{:valueType :db.type/instant}
           :end #:db{:valueType :db.type/instant}}))

;; TODO: this is highly cacheable
(defn current-attributes []
  (->> (j/q '{:db/ident _})
       (map #(into {} %))))

(defn current-schema []
  (->> (current-attributes)
       (encore/keys-by :db/ident)
       (encore/map-vals #(dissoc % :db/ident :db/valueType))))
(comment
 (current-attributes)
 (current-schema)
 (d/create-conn (current-schema)))

(defn add-schema
  "Accepts a schema map of new-attributes to add to an existing schema."
  [new-attributes]
  (assert (seq new-attributes)
          "must supply attributes")
  (assert (not-any? #(= :db.type/ref (:db/valueType %))
                    (vals new-attributes))
          "adding refs is not supported, use edges for relations")
  (assert (empty? (set/intersection (set (keys new-attributes))
                                    (set (j/q '(:db/ident _)))))
          "changes to existing attributes is not supported")
  ;; TODO: support type changes (e.g. str->int or int->str)
  (let [new-schema (merge base-schema
                          (current-schema)
                          (encore/map-vals #(dissoc % :db/valueType) new-attributes))]
    (when (not= new-schema (:schema @conn))
      (let [c (d/conn-from-datoms (or (d/datoms @conn :eavt) ()) new-schema)]
        #?(:cljs (set! conn c)
           :clj (alter-var-root #'conn (constantly c))))
      (j/attach conn)))
  ;; Insert the attributes into the DB to allow schema query and storage
  (j/transacte
   (for [[k v] new-attributes]
     (assoc v :db/ident k))))

;;;;
(add-schema default-schema)
(j/transacte seed/seed)
;;;;

;; TODO: generate from schema
(def ident
  {:node/type :type/name
   :edge/type :type/name
   :person/role :role/name})

(defn replace-idents [entity]
  (persistent!
   (reduce
    (fn [acc [k v]]
      (assoc! acc k (if-let [i (ident k)]
                      [i v]
                      v)))
    (transient {})
    entity)))

(defn add-entity [new-entity]
  (j/transacte [(replace-idents new-entity)]))

(defn retract-entity [entity]
  (j/transacte [[:db.fn/retractEntity (:db/id entity)]]))

(defn add-attribute [id k v]
  (j/transacte [[:db/add id k (if-let [i (ident k)]
                                [i v]
                                v)]]))

(defn retract-attribute [id k]
  (j/transacte [[:db.fn/retractAttribute id k]]))

;; TODO: Use schema? str/join smells
(defn vstr [x]
  (cond (de/entity? x)
        (if-let [namek (first (filter #(= (name %) "name") (keys x)))]
          (namek x)
          (str x))

        (set? x)
        (str/join ", " (sort (map vstr x)))

        (keyword? x)
        (name x)

        :else
        x))
