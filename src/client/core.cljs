(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.socket-io]))

(enable-console-print!)

(def HOST "https://boiling-shore-13036.herokuapp.com/")
;;(def HOST "http://localhost:5000")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text [:ul [:li "Hello world!"]]}))

(defn hello-world []
  (:text @app-state))

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

(defn SATELLITE
  "Take MIDI from foot switch, send up to server."
  []
  (when-MIDI (fn [] (when-let [keys (js/WebMidi.getInputByName "LPD8")]
                      (.addListener keys "noteon"  "all" #(do (js/console.log "noteon" %)
                                                              (.emit socket "to_server" #js {:midi (.-data %)})))
                      (.addListener keys "noteoff" "all" #(do (js/console.log "noteoff" %)
                                                              (.emit socket "to_server" #js {:midi (.-data %)})))))))

(defn LIST
  "List of MIDI devices."
  []
  (when-MIDI #(do
                (js/console.log "INPUTS"  (.-inputs js/WebMidi))
                (js/console.log "OUTPUTS" (.-outputs js/WebMidi))
                (reset! app-state {:text [:ul (map-indexed (fn [i item] [:li {:key i} (.-name item)]) (.-inputs js/WebMidi))]})

                )))

(.emit socket "to_server" #js {:hello "World"})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
