(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.socket-io]))

(enable-console-print!)

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

(def socket (io.connect "http://localhost:5000"))

(.on socket "to client"
     (fn [msg]
       (js/console.log "msg" msg)))

(defn consume [inputs]
  (when-let [n (.next inputs)]
    (when (not (.-done n))
      (conj (consume inputs) (.-value n)))))

(defn on-midi-message [msg]
  (do
    (js/console.log msg)
    (.emit socket "my event" #js {:midi (.-data msg)})))

(defn on-midi-success [midi-access]
  (js/console.log "MIDI access object: " midi-access)
  (js/console.log "inputs: " (clj->js (consume (.values (.-inputs midi-access)))))
  (doseq [i (consume (.values (.-inputs midi-access)))]
    (js/console.log (.-manufacturer i) (.-name i))
    (set! (.-onmidimessage i) on-midi-message)))

(defn on-midi-failure [e]
  (js/alert (str "No MIDI support in browser: " e)))

#_ (when-let [f (.-requestMIDIAccess js/navigator)]
  (.then (.requestMIDIAccess js/navigator #js {:sysex false}) on-midi-success on-midi-failure))

(let [socket (io.connect "http://localhost:5000")]
  (.on socket "connect" #(js/console.log "socket connected")))

(.enable js/WebMidi (fn [err] (if err
                                (js/alert (str "Could not enable MIDI" err))
                                (do
                                  (js/console.log (.-inputs js/WebMidi))
                                  (let [keys (js/WebMidi.getInputByName "LPD8")]
                                    (.addListener keys "noteon" "all" #(do (js/console.log "noteon" %)
                                                                           (.emit socket "my event" #js {:midi (.-data %)}))))))))
