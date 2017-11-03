(ns async-sound.sketch.1
  (:require [async-sound.core :as core]
            [clojure.core.async :as async]
            [quil.core :as q]
            [quil.middleware :as m]))

(def chan (async/chan (async/buffer 1)))

(def listener (core/listener {:chan chan :audio-format core/audio-format-mono-16 :name "ES8" :frame-rate 30}))

(defn map-sound [val]
  [(q/map-range val -2000 2000 0 255) 0 (q/map-range val 2000 -2000 0 255)])

(defn setup []
  (listener)
  {:bg-color 0})

(defn update-state [state]
  (let [[r g b] (map-sound (async/<!! chan))]
    {:r r :g g :b b}))

(defn draw-state [{r :r g :g b :b}]
  (q/background r g b))

(q/defsketch async-sound-1
  :title "async-sound-1"
  :size [1000 600]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:no-bind-output]
  :middleware [m/fun-mode])
