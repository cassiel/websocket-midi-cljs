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
       (js/console.log "msg" msg)))

(let [socket (io.connect HOST)]
  (.on socket "connect" #(js/console.log "socket connected")))

(.enable js/WebMidi (fn [err] (if err
                                (js/alert (str "Could not enable MIDI" err))
                                (do
                                  (js/console.log (.-inputs js/WebMidi))
                                  (when-let [keys (js/WebMidi.getInputByName "LPD8")]
                                    (.addListener keys "noteon" "all" #(do (js/console.log "noteon" %)
                                                                           (.emit socket "to_server" #js {:midi (.-data %)}))))))))

(.emit socket "to_server" #js {:hello "World"})
