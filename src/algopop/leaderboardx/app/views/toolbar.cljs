(ns algopop.leaderboardx.app.views.toolbar
  (:require
    [algopop.leaderboardx.app.io.csv :as csv]
    [algopop.leaderboardx.app.io.dot :as dot]
    [algopop.leaderboardx.graph.seed :as seed]
    [algopop.leaderboardx.graph.graph-settings :as settings]
    [algopop.leaderboardx.app.logging :as log]
    [clojure.string :as string]
    [reagent.core :as reagent]
    [algopop.leaderboardx.graph.graph :as graph]))

(defn settings [show-settings?]
  [:div.btn-group
   [:button.btn.btn-default.dropdown-toggle
    {:on-click
     (fn settings-click [e]
       (swap! show-settings? not))}
    [:span.glyphicon.glyphicon-cog {:aria-hidden "true"}]]])

(defn algo [show-algo?]
  [:div.btn-group
   [:button.btn.btn-default.dropdown-toggle
    {:on-click
     (fn settings-click [e]
       (swap! show-algo? not))}
    [:span.glyphicon.glyphicon-education {:aria-hidden "true"}]]])

;; TODO: make this open a panel like settings
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
      [:li "Enter a Person name and press ENTER to add them."]
      [:li "When entering a Person name, you can press tab and enter a comma separated list of nodes to link to and press ENTER to add them."]
      [:li "You can edit the lists of People and their links in the table."]
      [:li "Click on the table row to edit and add/remove/update names."]
      [:li "To delete nodes and links, click on the graph or table and press the DELETE key."]
      [:li "In the graph, select one name and shift click another to link them."]
      [:li "Shift click a selected node to change its shape."]
      [:li "Shift click a link to change the link type."]
      [:li "Drag nodes or edges around with the mouse."]
      [:li "Double click to unpin nodes and edges."]
      [:li "Email addresses will draw a Gravatar if one exists."]
      [:li "Clicking on a selected Person will zoom closer to them."]
      [:li "Click again to zoom closer, or click the background to unzoom and unselect."]
      [:li "Click on the cog icon in the top right to edit node types and edge types."]]]]])

(defn save-file [filename t s]
  (if js/Blob
    (let [b (js/Blob. #js [s] #js {:type t})]
      (if js/window.navigator.msSaveBlob
        (js/window.navigator.msSaveBlob b filename)
        (let [link (js/document.createElement "a")]
          (aset link "download" filename)
          (aset link "href" (js/window.URL.createObjectURL b))
          (aset link "onclick" (fn destroy-clicked [e]
                                 (.removeChild (.-body js/document) (.-target e))))
          (aset link "style" "display" "none")
          (.appendChild (.-body js/document) link)
          (.click link))))
    (log/error "Browser does not support Blob")))

(defn ends-with [s suffix]
  (not (neg? (.indexOf s suffix (- (.-length s) (.-length suffix))))))

(defn read-file [g file read-graph]
  (if js/FileReader
    (let [reader (js/FileReader.)]
      (set! (.-onload reader)
            (fn csv-loaded [e]
              (when-let [new-graph (read-graph (.. e -target -result))]
                (reset! g new-graph))))
      (.readAsText reader file))
    (log/error "Browser does not support FileReader")))

;; TODO: not sure I like and-then
(defn import-button [label accept and-then g]
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
            (do (graph/with-ranks (read-file g file r))
                (and-then))
            (log/error "Must supply a .dot or .txt file"))))}]]])

(defn action-button [label f]
  [:li [:a.btn {:on-click f} label]])

(defn filename [{:keys [title]} ext]
  (str (or title "graph") "." ext))

(defn format-svg [svg]
  (str
    "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">"
    (string/replace svg #" data-reactid=\"[^\"]*\"" "")
    "</svg>"))

(defn toolbar [g get-svg show-settings? show-algo? selected-id selected-node-type selected-edge-type]
  [:div.btn-toolbar.pull-right {:role "toolbar"}
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Load"]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Empty"
      (fn clear-click [e]
        (seed/set-empty! g )
        ;; maybe only if doesn't exist anymore
        ;; maybe centralize
        (reset! selected-id nil)
        (reset! selected-node-type (first (keys (:node-types @g))))
        (reset! selected-edge-type (first (keys (:edge-types @g)))))]
     [action-button "Random"
      (fn random-click [e]
        (seed/set-rand! g)
        (reset! selected-id nil)
        (reset! selected-node-type (first (keys (:node-types @g))))
        (reset! selected-edge-type (first (keys (:edge-types @g)))))]
     [action-button "Example"
      (fn random-click [e]
        (seed/set-example! g)
        (reset! selected-id nil)
        (reset! selected-node-type (first (keys (:node-types @g))))
        (reset! selected-edge-type (first (keys (:edge-types @g)))))]
     [import-button "File (dot or txt)" ".dot,.txt"
      (fn [x]
        (reset! selected-id nil)
        (reset! selected-node-type (first (keys (:node-types @g))))
        (reset! selected-edge-type (first (keys (:edge-types @g)))))
      g]]]
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Save"]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Graph (dot)"
        (fn export-graphviz [e]
          (save-file (filename @g "dot") "text/dot" (dot/write-graph @g)))]
     [action-button "Summary table (txt)"
        (fn export-csv-click [e]
          (save-file (filename @g "txt") "text/csv" (csv/write-graph @g)))]
     [action-button "Image (svg)"
      (fn export-svg [e]
        (save-file (filename @g "svg") "image/svg+xml" (format-svg (get-svg))))]]]
   [help]
   [algo show-algo?]
   [settings show-settings?]])
