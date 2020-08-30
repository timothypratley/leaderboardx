(ns algopop.leaderboardx.app.model.seed)

(def seed
  [#:person{:name "Suzzie"
            :height 180}
   #:person{:name "Sally"
            :height 165}
   #:edge{:from [:person/name "Suzzie"]
          :to [:person/name "Sally"]}])
