(ns algopop.leaderboardx.roster.seed
  (:require [justice.core :as j]))

(defn set-simple-example! []
  (j/transacte
   [#:role{:name "Support"}
    #:role{:name "Teacher"}
    #:role{:name "Coordinator"}

    #:duty{:name "Morning Bus"
           :start "08:00"}
    #:edge{:from [:duty/name "Morning Bus"]
           :to [:role/name "Support"]}
    #:duty{:name "Recess"
           :start "11:00"}
    #:duty{:name "Lunch"
           :start "13:00"}
    #:duty{:name "Library"
           :start "13:00"}
    #:duty{:name "Evening Bus"
           :start "15:00"}

    #:person{:name "Kate"}
    #:edge{:from [:person/name "Kate"]
           :to [:role/name "Coordinator"]}
    #:person{:name "Kim"}
    #:edge{:from [:person/name "Kim"]
           :to [:role/name "Teacher"]}
    #:person{:name "Kirstin"}
    #:edge{:from [:person/name "Kirstin"]
           :to [:role/name "Teacher"]}
    #:person{:name "Barry"}
    #:edge{:from [:person/name "Barry"]
           :to [:role/name "Teacher"]}
    #:person{:name "Brian"}
    #:edge{:from [:person/name "Brian"]
           :to [:role/name "Support"]}
    #:person{:name "Bob"}
    #:edge{:from [:person/name "Bob"]
           :to [:role/name "Support"]}]))

(defn set-example! []
  (j/transacte
   [#:role{:name "Support"}
    #:role{:name "Teacher"}
    #:role{:name "Coordinator"}

    #:duty{:name "Morning Bus"
           :start "08:15"
           :duration 30
           :roles #{"Support"}}
    #:duty{:name "Morning"
           :start-time "08:15"
           :duration 30}
    #:duty{:name "Recess Area 1"
           :start-time "10:50"
           :duration 30}
    #:duty{:name "Recess Area 2"
           :start-time "10:50"
           :duration 30}
    #:duty{:name "Recess Area 3"
           :start-time "10:50"
           :duration 30}
    #:duty{:name "Recess Library"
           :start-time "10:50"
           :duration 30}
    #:duty{:name "K-1 Eating Area"
           :start-time "12:50"
           :duration 10}
    #:duty{:name "2-5 Eating Area"
           :start-time "12:50"
           :duration 10}
    #:duty{:name "Lunch Area 1"
           :start-time "13:00"
           :duration 30}
    #:duty{:name "Lunch Area 2"
           :start-time "13:00"
           :duration 30}
    #:duty{:name "Lunch Area 3"
           :start-time "13:00"
           :duration 30}
    #:duty{:name "Homework Club"
           :start-time "13:00"
           :duration 30}
    #:duty{:name "Library"
           :start-time "13:00"
           :duration 30}
    #:duty{:name "Fitzroy Extra"
           :start-time "13:00"
           :duration 30}
    #:duty{:name "Curb Pickup"
           :start-time "15:05"
           :duration 35}
    #:duty{:name "Fort Farwell"
           :start-time "15:05"
           :duration 35}
    #:duty{:name "Bus Lines 2"
           :start-time "15:05"
           :duration 20}
    #:duty{:name "Bus 2"
           :start-time "15:20"
           :duration 30}
    #:duty{:name "Afternoon Homework Club"
           :start-time "15:20"
           :duration 30}

    #:duty{:name "Evening Bus"
           :start-time "15:00"}
    #:person{:name "Liz"}
    #:edge{:from [:person/name "Liz"]
           :to [:role/name "Support"]}
    #:person{:name "Kristy"}
    #:edge{:from [:person/name "Kristy"]
           :to [:role/name "Teacher"]}
    #:person{:name "Jeremy"}
    #:edge{:from [:person/name "Jeremy"]
           :to [:role/name "Teacher"]}
    #:person{:name "Mandy"}
    #:edge{:from [:person/name "Mandy"]
           :to [:role/name "Coordinator"]}
    #:edge{:from [:person/name "Amanda"]
           :to [:role/name "Teacher"]}
    #:edge{:from [:person/name "Paul"]
           :to [:role/name "Support"]}
    (comment
     "Kelly" {:node/role "Coordinator"}
     "Janelle" {:node/role "Teacher"}
     "Irene" {:node/role "Support"}
     "Amy" {:node/role "Teacher"}
     "Bronwyn" {:node/role "Teacher"}
     "Gloria" {:node/role "Teacher"}
     "Jodie" {:node/role "Coordinator"}
     "Jodi" {:node/role "Teacher"}
     "Robyn" {:node/role "Teacher"}
     "Violeta" {:node/role "Teacher"}
     "Andrew" {:node/role "Coordinator"}
     "Alison" {:node/role "Teacher"}
     "Marina" {:node/role "Support"}
     "Svet" {:node/role "Support"}
     "Jon" {:node/role "Teacher"}
     "Aminata" {:node/role "Teacher"}
     "Rowena" {:node/role "Teacher"}
     "Sheree" {:node/role "Support"}
     "Terrence" {:node/role "Support"})
    ]

   ))
