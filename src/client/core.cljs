(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.socket-io]))

(enable-console-print!)

(def HOST "https://boiling-shore-13036.herokuapp.com/")
;;(def HOST "http://localhost:5000")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn hello-world []
  [:h1 (:text @app-state)])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

(def socket (io.connect HOST))

(.on socket "to_client"
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
              (js/console.log "other status")))))))

#_ (let [socket (io.connect HOST)]
  (.on socket "connect" #(js/console.log "socket connected")))

(.enable js/WebMidi (fn [err] (if err
                                (js/alert (str "Could not enable MIDI" err))
                                (do
                                  (js/console.log "INPUTS"  (.-inputs js/WebMidi))
                                  (js/console.log "OUTPUTS" (.-outputs js/WebMidi))

                                  (when-let [keys (js/WebMidi.getInputByName "LPD8")]
                                    (.addListener keys "noteon"  "all" #(do (js/console.log "noteon" %)
                                                                            (.emit socket "to_server" #js {:midi (.-data %)})))
                                    (.addListener keys "noteoff" "all" #(do (js/console.log "noteoff" %)
                                                                            (.emit socket "to_server" #js {:midi (.-data %)})))                                    )))))

(.emit socket "to_server" #js {:hello "World"})
