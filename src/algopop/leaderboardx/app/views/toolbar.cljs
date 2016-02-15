(ns algopop.leaderboardx.app.views.toolbar
  (:require ;;[algopop.leaderboardx.app.io.dot :as dot]
            ;;[algopop.leaderboardx.app.io.csv :as csv]
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
      [:li "Select one name and shift click another to add a link."]
      [:li "Shift click a selected node to change its shape."]
      [:li "Shift click a link to make it dashed."]
      [:li "Drag nodes or edges around with the mouse."]
      [:li "Double click to unpin nodes and edges."]
      [:li "Click on the table row then click again to edit."]
      [:li "If your names are email addresses, a Gravatar will be drawn."]]]]])

(defn save-file [filename t s]
  (if js/Blob
    (let [b (js/Blob. #js [s] #js {:type t})]
      (if js/window.navigator.msSaveBlob
        (js/window.navigator.msSaveBlob b filename)
        (let [link (js/document.createElement  "a")]
          (aset link "download" filename)
          (if js/window.webkitURL
            (aset link "href" (js/window.webkitURL.createObjectURL b))
            (do
              (aset link "href" (js/window.URL.createObjectURL b))
              (aset link "onclick" (fn destroy-clicked [e]
                                     (.removeChild (.-body js/document) (.-target e))))
              (aset link "style" "display" "none")
              (.appendChild (.-body js/document) link)))
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
          #_(if-let [r (cond (ends-with (.-name file) ".txt") csv/read-graph
                          (ends-with (.-name file) ".dot") dot/read-graph)]
            (read-file g file r)
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

(defn toolbar [g get-svg]
  [:div.btn-toolbar.pull-right {:role "toolbar"}
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Load"]
    [:ul.dropdown-menu {:role "menu"}
     [action-button "Empty"
      (fn clear-click [e]
        ;; TODO: how to delete?
        (reset! g {:nodes {}
                   :edges {}}))]
     [action-button "Random"
      (fn random-click [e]
        (seed/set-rand!))]
     [action-button "Example"
      (fn random-click [e]
        (seed/set-example!))]
     #_[import-button "File (dot or txt)" ".dot,.txt" dot/read-graph g]]]
   [:div.btn-group
    [:button.btn.btn-default.dropdown-toggle
     {:data-toggle "dropdown"
      :aria-expanded "false"}
     "Save"]
    [:ul.dropdown-menu {:role "menu"}
     #_[action-button "Graph (dot)"
      (fn export-graphviz [e]
        (save-file (filename @g "dot") "text/dot" (dot/write-graph @g)))]
     #_[action-button "Summary table (txt)"
      (fn export-csv-click [e]
        (save-file (filename @g "txt") "text/csv" (csv/write-graph @g)))]
     [action-button "Image (svg)"
      (fn export-svg [e]
        (save-file (filename @g "svg") "image/svg+xml" (format-svg (get-svg))))]]]
   [help]])
