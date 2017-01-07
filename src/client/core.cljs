(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.socket-io]))

(enable-console-print!)

(def HOST "https://boiling-shore-13036.herokuapp.com/")
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
                       (when-let [output (.getOutputByName js/WebMidi "to Max 1")]
                         (js/console.log "bytes" midi-bytes)
                         (let [status (bit-and (aget midi-bytes 0) 0xF0)
                               pitch (aget midi-bytes 1)]
                           (js/console.log "status" status "pitch" pitch)
                           (case status
                             0x90 (do (js/console.log "< note on" pitch)
                                      (.playNote output pitch 1))
                             0x80 (do (js/console.log "< note off" pitch)
                                      (.stopNote output pitch 1))
                             (js/console.log "other status")))))))))

(defn show-latch [how]
  (swap! app-state
         assoc-in [:content 1]
         [:div.row [:div.col-md-12 [:h2 (if how "ON" "OFF")]]]))

(defn note-on-normal [midi]
  (.emit socket "to_server" #js {:midi midi})
  (show-latch true))

(defn note-off-normal [midi]
  (.emit socket "to_server" #js {:midi midi})
  (show-latch false))

(defn note-on-latch [midi]
  (let [status (aget midi 0)
        pitch (aget midi 1)
        velocity (aget midi 2)
        how (get-in (swap! app-state update-in [:state :latch] not)
                    [:state :latch])
        ]
    (js/console.log "latch data" (clj->js [status pitch (if how velocity 0)]))
    (.emit socket "to_server" (clj->js {:midi [status pitch (if how velocity 0)]}))
    (show-latch how)))

(defn note-off-latch [midi]
  )

(def note-on (if LATCHING note-on-latch note-on-normal))
(def note-off (if LATCHING note-off-latch note-off-normal))

(defn SATELLITE
  "Take MIDI from foot switch, send up to server."
  []
  (when-MIDI (fn [] (when-let [keys (js/WebMidi.getInputByName "LPD8")]
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
                      (show-latch false)
                      (.addListener keys "noteon"  "all" #(do (js/console.log "noteon" %)
                                                              (note-on (.-data %))))
                      (.addListener keys "noteoff" "all" #(do (js/console.log "noteoff" %)
                                                              (note-off (.-data %))))))))

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

(SATELLITE)
