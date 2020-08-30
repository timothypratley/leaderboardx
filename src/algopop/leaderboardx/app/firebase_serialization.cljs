(ns algopop.leaderboardx.app.firebase-serialization
  (:require
    [clojure.string :as str])
  (:require-macros
    [cljs.test :refer [deftest is]]))

(def special-characters
  ".$[]#/%")

(def special-codes
  (for [c special-characters]
    (str "%" (-> c (.charCodeAt 0) (.toString 16) (.toUpperCase)))))

(def charToCode
  (zipmap special-characters special-codes))

(def codeToChar
  (zipmap special-codes special-characters))

(def escape-regex
  (js/RegExp. (str/join "|" (for [c special-characters]
                                 (str "\\" c)))
              "g"))

(defn escape [s]
  (.replace s escape-regex #(charToCode %)))

(def unescape-regex
  (js/RegExp. (str/join "|" special-codes) "g"))

(defn unescape [s]
  (if (string? s)
    (.replace s unescape-regex #(codeToChar %))
    s))

(defn clj->firebase [m]
  (clj->js
    (if (map? m)
      (into
        {}
        (for [[k v] m]
          [(if (number? k)
             k
             (escape (str k)))
           (clj->firebase v)]))
      m)))

(defn maybe-keyword [s]
  (if (and (string? s)
           (str/starts-with? s ":"))
    (keyword (subs s 1))
    s))

(defn firebase->clj [m]
  (let [m (js->clj m)]
    (if (map? m)
      (into
        {}
        (for [[k v] m]
          [(maybe-keyword (unescape k)) (firebase->clj v)]))
      m)))

(deftest serialization-test
         (let [m {:foo.bar/booz {"baz%?" 2}}]
           (is (= m (firebase->clj (clj->firebase m))))))

