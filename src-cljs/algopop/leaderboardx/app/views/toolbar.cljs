(ns algopop.leaderboardx.app.views.toolbar
  (:require [algopop.leaderboardx.app.io.dot :as dot]
            [algopop.leaderboardx.app.io.csv :as csv]
            [algopop.leaderboardx.app.seed :as seed]
            [algopop.leaderboardx.app.logging :as log]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(defn help []
  [:div.btn-group
   [:button.btn.btn-default.dropdown-toggle
    {:data-toggle "dropdown"
     :aria-expanded "false"}
    [:span.glyphicon.glyphicon-question-sign {:aria-hidden "true"}]]
   [:div.panel.panel-default.dropdown-menu.dropdown-menu-right
    {:style {:width "550px"}}
    [:div.panel-body
     [:ul.list-unstyled
      [:li "Enter a node name and press ENTER to add it."]
      [:li "Enter a comma separated list of nodes to link to and press ENTER to add them."]
      [:li "To delete nodes and links, click on the graph or table and press the DELETE key."]
      [:li "Drag nodes or edges around with the mouse."]
      [:li "Double click to unpin nodes and edges."]
      [:li "Click on the table row then click again to edit."]]]]])

(defn save-file [filename str]
  (let [link (.createElement js/document "a")]
    (aset link "download" filename)
    (aset link "href" (js/encodeURI str))
    (.click link)))

(defn ends-with [s suffix]
  (not (neg? (.indexOf s suffix (- (.-length s) (.-length suffix))))))

(defn read-file [g file read-graph]
  (let [reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn csv-loaded [e]
            (when-let [new-graph (read-graph (.. e -target -result))]
              (reset! g new-graph))))
    (.readAsText reader file)))

(defn import-button [label accept read-graph g]
  [:li
   [:a.btn.btn-file
    label
    [:input
     {:type "file"
      :name "import"
      :tab-index "-1"
      :accept accept
      :value ""
      :on-change
      (fn import-csv-change [e]
        (when-let [file (aget e "target" "files" 0)]
          (if-let [r (cond (ends-with (.-name file) ".txt") csv/read-graph
                          (ends-with (.-name file) ".dot") dot/read-graph)]
            (read-file g file r)
            (log/error "Must supply a .dot or .txt file"))))}]]])

(defn action-button [label f]
  [:li [:a.btn {:on-click f} label]])

(defn filename [{:keys [title]} ext]
  (str (or title "graph") "." ext))

(defn toolbar [g get-svg]
  [:div.btn-toolbar.pull-right {:role "toolbar"}
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Load "
     [:span.caret]]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Empty"
      (fn clear-click [e]
        (reset! g {:nodes {"Root" {}}
                   :edges {"Root" {}}}))]
     [action-button "Random"
      (fn random-click [e]
        (reset! g (seed/rand-graph)))]
     [import-button "File (dot or txt)" ".dot,.txt" dot/read-graph g]]]
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Save "
     [:span.caret]]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Graph (dot)"
      (fn export-graphviz [e]
        (save-file (filename @g "dot") (str "data:text/dot;charset=utf-8," (dot/write-graph @g))))]
     [action-button "Summary table (txt)"
      (fn export-csv-click [e]
        (save-file (filename @g "txt") (str "data:text/csv;charset=utf-8," (csv/write-graph @g))))]
     [action-button "Image (svg)"
      (fn export-svg [e]
        (save-file (filename @g "svg")
                   (str "data:image/svg+xml;utf8,"
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">"
                        (string/replace (get-svg) #" data-reactid=\"[^\"]*\"" "")
                        "</svg>")))]]]
   [help]])
