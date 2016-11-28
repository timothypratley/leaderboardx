(ns algopop.leaderboardx.app.seed
  (:require [clojure.string :as str]
            [algopop.leaderboardx.app.db :as db]))

(def names
  ["Emma" "Noah" "Olivia" "Liam" "Sophia" "Mason" "Isabella" "Jacob"
   "Ava" "William" "Mia" "Ethan" "Emily" "Michael" "Abigail" "Alexander"
   "Madison" "James" "Charlotte" "Daniel" "Harper" "Elijah" "Sofia" "Benjamin"
   "Avery" "Logan" "Elizabeth" "Aiden" "Amelia" "Jayden" "Evelyn" "Matthew"
   "Ella" "Jackson" "Chloe" "David" "Victoria" "Lucas" "Aubrey" "Joseph"
   "Grace" "Anthony" "Zoey" "Andrew" "Natalie" "Samuel" "Addison" "Gabriel"
   "Lillian" "Joshua" "Brooklyn" "John" "Lily" "Carter" "Hannah" "Luke"
   "Layla" "Dylan" "Scarlett" "Christopher" "Aria" "Isaac" "Zoe" "Oliver"
   "Samantha" "Henry" "Anna" "Sebastian" "Leah" "Caleb" "Audrey" "Owen"
   "Ariana" "Ryan" "Allison" "Nathan" "Savannah" "Wyatt" "Arianna" "Hunter"
   "Camila" "Jack" "Penelope" "Christian" "Gabriella" "Landon" "Claire" "Jonathan"
   "Aaliyah" "Levi" "Sadie" "Jaxon" "Riley" "Julian" "Skylar" "Isaiah"
   "Nora" "Eli" "Sarah" "Aaron" "Hailey" "Charles" "Kaylee" "Connor"
   "Paisley" "Cameron" "Kennedy" "Thomas" "Ellie" "Jordan" "Peyton" "Jeremiah"
   "Annabelle" "Nicholas" "Caroline" "Evan" "Madelyn" "Adrian" "Serenity" "Gavin"
   "Aubree" "Robert" "Lucy" "Brayden" "Alexa" "Grayson" "Alexis" "Josiah"
   "Nevaeh" "Colton" "Stella" "Austin" "Violet" "Angel" "Genesis" "Jace"
   "Mackenzie" "Dominic" "Bella" "Kevin" "Autumn" "Brandon" "Mila" "Tyler"
   "Kylie" "Parker" "Maya" "Ayden" "Piper" "Jason" "Alyssa" "Jose"
   "Taylor" "Ian" "Eleanor" "Chase" "Melanie" "Adam" "Naomi" "Hudson"
   "Faith" "Nolan" "Eva" "Zachary" "Katherine" "Easton" "Lydia" "Blake"
   "Brianna" "Jaxson" "Julia" "Cooper" "Ashley" "Lincoln" "Khloe" "Xavier"
   "Madeline" "Bentley" "Ruby" "Kayden" "Sophie" "Carson" "Alexandra" "Brody"
   "London" "Asher" "Lauren" "Nathaniel" "Gianna" "Ryder" "Isabelle" "Justin"
   "Alice" "Leo" "Vivian" "Juan" "Hadley" "Luis"])

(defn rand-graph []
  (let [ks (take 10 (shuffle names))
        nodes (into {} (for [k ks]
                         [k {:hair (rand-nth ["red" "brown" "black" "blonde"])}]))]
    {:nodes nodes
     :edges (for [from ks]
              [from (into {} (for [to (take 2 (shuffle (remove #{from} ks)))]
                               [to {:value 1}]))])}))

(defn set-rand! []
  (doseq [[k outs] (:edges (rand-graph))]
    (db/replace-edges k outs nil)))

(def example
  {:edges ["Amy" ["Lily", "Abigail", "Emma"],
           "Rhys" ["William", "Liam", "Matt"],
           "Noah" ["William", "Matt"],
           "Michael" ["William", "Mason", "Abigail"],
           "Toby" ["Joel", "Mason", "Alex"],
           "Olivia" ["Mia", "Claire", "Charlotte"],
           "Jayden" ["Rhys", "Liam", "Matt"],
           "Madison" ["Isabella", "Emily", "William"],
           "Daniel" ["Mason", "Jayden", "Sophia"],
           "Mia" ["Emily", "Olivia", "Claire"],
           "William" ["Matt", "Emily", "Noah"],
           "Matt" ["Liam", "Jayden", "William"],
           "Claire" ["Charlotte", "Olivia", "Emily"],
           "Sophia" ["Emma", "Abigail", "Rachelle"],
           "Emma" ["Abigail", "Sophia", "Amy"],
           "Joel" ["Emily", "Alex", "Isabella"],
           "Emily" ["Mia", "Joel", "William"],
           "Abigail" ["Rachelle", "Amy", "Sophia"],
           "Alex" ["Mason", "Joel", "Daniel"],
           "Isabella" ["Madison", "Emily", "Mia"],
           "Lily" ["Amy", "Sophia", "Rachelle"],
           "Charlotte" ["Olivia", "Emily", "Claire"],
           "Liam" ["Matt", "Jayden", "William"],
           "Rachelle" ["Abigail", "Sophia", "Emma"],
           "Mason" ["Alex", "Daniel", "Toby"]],
   :title "Example"})

(defn set-example! []
  (doseq [[k outs] (partition-all 2 (:edges example))]
    (db/replace-edges k outs nil)))
