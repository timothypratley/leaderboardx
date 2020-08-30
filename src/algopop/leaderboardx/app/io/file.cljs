(ns algopop.leaderboardx.app.io.file)

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
    (js/alert "Browser does not support Blob")))

(defn ends-with [s suffix]
  (not (neg? (.indexOf s suffix (- (.-length s) (.-length suffix))))))

(defn read-file [r file deserialize]
  (if js/FileReader
    (let [reader (js/FileReader.)]
      (set! (.-onload reader)
            (fn csv-loaded [e]
              (when-let [new-graph (deserialize (.. e -target -result))]
                (reset! r new-graph))))
      (.readAsText reader file))
    (js/alert "Browser does not support FileReader")))
