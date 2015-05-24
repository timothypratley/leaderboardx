(ns algopop.leaderboardx.app.views.toolbar
  (:require [algopop.leaderboardx.app.io.dot :as dot]
            [algopop.leaderboardx.app.io.csv :as csv]
            [algopop.leaderboardx.app.seed :as seed]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(defn help []
  (let [show-help (reagent/atom false)]
    (fn a-help []
      [:div.pull-right
       [:span.btn.btn-default.pull-right
        {:on-click (fn help-click [e]
                     (swap! show-help not))}
        [:span.glyphicon.glyphicon-question-sign {:aria-hidden "true"}]]
       (when @show-help
         [:div.panel.panel-default
          {:on-click (fn help-panel-click [e]
                       (swap! show-help not))}
          [:div.panel-body
           [:ul.list-unstyled
            [:li "Enter a node name and press ENTER to add it."]
            [:li "Enter a comma separated list of nodes to link to and press ENTER to add them."]
            [:li "Select a node or edge by mouse clicking it and press DEL to delete it."]
            [:li "Drag nodes or edges around by click hold and move."]
            [:li "Double click to unpin nodes and edges."]]]])])))

(defn save-file [filename str]
  (let [link (.createElement js/document "a")]
    (set! (.-download link) filename)
    (set! (.-href link) (js/encodeURI str))
    (.click link)))

(defn import-button [label accept read-graph g]
  [:span.btn.btn-default.btn-file
   label
   [:input
    {:type "file"
     :name "import"
     :accept accept
     :on-change (fn import-csv-change [e]
                  (when-let [file (aget e "target" "files" 0)]
                    (let [reader (js/FileReader.)]
                      (set! (.-onload reader)
                            (fn csv-loaded [e]
                              (reset! g (read-graph (.. e -target -result)))))
                      (.readAsText reader file))))}]])

(defn toolbar [g get-svg]
  [:div
   [:span.btn.btn-default
    {:on-click (fn clear-click [e]
                 (reset! g {:nodes {"root" {}}
                            :edges {"root" {}}}))}
    "Clear"]

   [:span.btn.btn-default
    {:on-click (fn random-click [e]
                 (reset! g (seed/rand-graph)))}
    "Random"]

   [import-button "Import CSV" "text/csv" csv/read-graph g]

   [import-button "Import Graphviz" "text/dot" dot/read-graph g]

   [:button.btn.btn-default
    {:on-click (fn export-csv-click [e]
                 (save-file "graph.csv"
                            (str "data:text/csv;charset=utf-8," (csv/write-graph @g))))}
    "Export CSV"]

   [:button.btn.btn-default
    {:on-click (fn export-graphviz [e]
                 (save-file "graph.dot"
                            (str "data:text/dot;charset=utf-8," (dot/write-graph @g))))}
    "Export Graphviz"]

   [:button.btn.btn-default
    {:on-click (fn export-svg [e]
                 (save-file "graph.svg"
                            (str "data:image/svg+xml;utf8,"
                                 "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">"
                                 (string/replace (get-svg) #" data-reactid=\"[^\"]*\"" "")
                                 "</svg>")))}
    "Export SVG"]
   [help]])
