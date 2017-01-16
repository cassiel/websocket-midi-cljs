(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.socket-io]))

(enable-console-print!)

;;(def HOST "https://boiling-shore-13036.herokuapp.com/")
(def HOST "https://identity-noise.herokuapp.com/")
;;(def HOST "http://localhost:5000")

(def LATCHING true)                     ; note-on to toggle behaviour...

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:content [:ul [:li "Hello world!"]]
                          :state {:latch false}}))

(defn hello-world []
  (:content @app-state))

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

;; The same compiled JS is used in every page. The page itself calls the top-level function to do the work.
;; (However, every page has a socket to the server.)

(def socket (io.connect HOST))

(defn note-name [pitch]
  (let [octave (int (/ pitch 12))
        note (mod pitch 12)
        names ["C" "C#" "D" "D#" "E" "F"
               "F#" "G" "G#" "A" "A#" "B"]]
    (str (names note)
         (- octave 2))))

(defn when-MIDI [f]
  (.enable js/WebMidi (fn [err]
                        (if err
                          (js/alert (str "Could not enable MIDI" err))
                          (f)))))

(defn MAIN_SITE
  "Respond to MIDI coming in from the server, send to modular."
  []
  (when-MIDI #(.on socket "to_client"
                   (fn [msg]
                     (js/console.log "msg to client" msg)
                     (when-let [midi-bytes (.-midi msg)]
                       (if-let [output (.getOutputByName js/WebMidi "to Max 1")]
                         (do
                           (js/console.log "bytes" midi-bytes)
                           (let [status (bit-and (aget midi-bytes 0) 0xF0)
                                 pitch (aget midi-bytes 1)
                                 velocity (aget midi-bytes 2)]
                             (js/console.log "status" status "pitch" pitch)
                             (case status
                               0x90 (if (= velocity 0)
                                      (do (js/console.log "< note off" pitch)
                                          (.stopNote output pitch 1))
                                      (do (js/console.log "< note on" pitch)
                                          (.playNote output pitch 1)))
                               0x80 (do (js/console.log "< note off" pitch)
                                        (.stopNote output pitch 1))
                               (js/console.log "other status"))))
                         (js/console.log (str "Can't find " "to Max 1"))))))))

(defn show-latch [how pitch]
  (swap! app-state
         assoc-in [:content 1]
         [:div.row [:div.col-md-12 [:h2 (if how "ON: " "OFF: ") (note-name pitch)]]]))

(defn note-on-normal [midi]
  (.emit socket "to_server" #js {:midi midi})
  (show-latch true (aget midi 1)))

(defn note-off-normal [midi]
  (.emit socket "to_server" #js {:midi midi})
  (show-latch false (aget midi 1)))

(defn note-on-latch [midi]
  (let [status (aget midi 0)
        pitch (aget midi 1)
        velocity (aget midi 2)
        how (get-in (swap! app-state update-in [:state :latch] not)
                    [:state :latch])
        ]
    (js/console.log "latch data" (clj->js [status pitch (if how velocity 0)]))
    (.emit socket "to_server" (clj->js {:midi [status pitch (if how velocity 0)]}))
    (show-latch how pitch)))

(defn note-off-latch [midi]
  )

(def note-on (if LATCHING note-on-latch note-on-normal))
(def note-off (if LATCHING note-off-latch note-off-normal))

(def DEVICES ["from Max 1" "Logidy UMI3"])

(defn SATELLITE
  "Take MIDI from foot switch, send up to server."
  []
  (when-MIDI (fn [] (if-let [keys (reduce (fn [result name] (or result
                                                                (js/WebMidi.getInputByName name)))
                                          nil DEVICES)]
                      (do
                       (swap! app-state
                              assoc :content
                              [:div
                               [:div.row [:div.col-md-12 [:h2 "OFF"]]]
                               [:div.row [:div.col-md-12 [:iframe {:src "http://ipcamlive.com/player/player.php?alias=57c7d74347fa1"
                                                                   :width "100%"
                                                                   :height "100%"
                                                                   :frameBorder 0}]]]
                               ]
                              )
                       (show-latch false 0)
                       (.addListener keys "noteon"  "all" #(do (js/console.log "noteon" %)
                                                               (note-on (.-data %))))
                       (.addListener keys "noteoff" "all" #(do (js/console.log "noteoff" %)
                                                               (note-off (.-data %)))))
                      (swap! app-state
                             assoc :content
                             [:div
                              [:div.row [:div.col-md-12 [:h2 "Cannot find device"]]]
                              [:div.row [:div.col-md-12 [:ul (map-indexed (fn [i t] [:li {:key i} "Tried: " t]) DEVICES)]]]
                              [:div.row [:div.col-md-12 [:iframe {:src "http://ipcamlive.com/player/player.php?alias=57c7d74347fa1"
                                                                  :width "100%"
                                                                  :height "100%"
                                                                  :frameBorder 0}]]]
                              ])))))

(defn LIST
  "List of MIDI devices."
  []
  (when-MIDI #(do
                (js/console.log "INPUTS"  (.-inputs js/WebMidi))
                (js/console.log "OUTPUTS" (.-outputs js/WebMidi))
                (swap! app-state
                       assoc :content
                       [:div
                        [:div.row [:div.col-md-12 [:h2 "INPUTS"]]]
                        [:div (map-indexed (fn [i item] [:div.row {:key i} [:div.col-md-12 (.-name item)]])
                                           (.-inputs js/WebMidi))]
                        [:div.row [:div.col-md-12 [:h2 "OUTPUTS"]]]
                        [:div (map-indexed (fn [i item] [:div.row {:key i} [:div.col-md-12 (.-name item)]])
                                           (.-outputs js/WebMidi))]]))))

(.emit socket "to_server" #js {:hello "World"})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

;;(SATELLITE)
