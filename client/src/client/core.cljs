(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "This text is printed from src/client/core.cljs. Go ahead and edit it and see reloading in action.")

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

(defn consume [inputs]
  (when-let [n (.next inputs)]
    (when (not (.-done n))
      (conj (consume inputs) (.-value n)))
    )
  )

(defn on-midi-message [msg]
  (js/console.log msg))

(defn on-midi-success [midi-access]
  (js/console.log "MIDI access object: " midi-access)
  (js/console.log "inputs: " (clj->js (consume (.values (.-inputs midi-access)))))
  (doseq [i (consume (.values (.-inputs midi-access)))]
    (set! (.-onmidimessage i) on-midi-message)
    )

  )

(defn on-midi-failure [e]
  (js/alert (str "No MIDI support in browser: " e))
  )

(when-let [f (.-requestMIDIAccess js/navigator)]
  (.then (.requestMIDIAccess js/navigator #js {:sysex false}) on-midi-success on-midi-failure)
  )
