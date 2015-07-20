(ns algopop.leaderboardx.app.db
  (:require [algopop.leaderboardx.app.views.common :as common]
            [datascript :as d]))

(defonce schema {})
(defonce conn (d/create-conn schema))

(defn add-assessment [coach player attrs]
  (d/transact! conn [{:coach coach
                      :player player
                      :attrs attrs}]))

(defonce seed
  (do
    (d/transact! conn [{:name "William"
                        :somedata "something about William" }])

    (add-assessment "Coach" "William" {:producivity 7})
    (d/transact! conn [{:assessment-template :player-assessment
                        :components [[:metrics "Metrics" ["Productivity" "Leadership" "Happiness"]]
                                     [:ol "Achievements"]
                                     [:ol "Weaknesses"]
                                     [:ol "Coach goals"]
                                     [:ol "Player goals"]
                                     [:textarea "Coach comments"]
                                     [:textarea "Player comments"]]}])))


(def q-player
  '[:find ?s ?attrs (pull ?e [*])
    :where [?a :coach "Coach"]
    [?a :player "William"]
    [?a :attrs ?attrs]
    [?e :name "William"]
    [?e :somedata ?s]])

(defn player []
  (common/bind conn q-player))

(def q-ac
  `[:in $ ?template
    :find ?c
    :where
    [$ ?e :assessment-template ?template]
    [$ ?e :components ?c]])

(defn assessment-components []
  (common/bind conn q-ac))

;;(println (d/q q-ac @conn :player-assessment))

(defn insert []
  (d/transact!
   conn
   [{:db/id 3
     :achievements #{"Won the spelling bee."}
     :components [[:metrics "Metrics" ["Productivity"
                                       "Leadership"
                                       "Happiness"]]
                  [:ol "Achievements" ["Won the spelling bee."]]
                  [:ol "Weaknesses"]
                  [:ol "Coach goals"]
                  [:ol "Player goals"]
                  [:textarea "Coach comments"]
                  [:textarea "Player comments"]]}]))
