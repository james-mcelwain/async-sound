(ns async-sound.sketch.1
  (:require [async-sound.core :as core]
            [clojure.core.async :as async]
            [quil.core :as q]
            [quil.middleware :as m]))

(def chan (async/chan (async/buffer 1)))
(def listener (core/listener {:chan chan :audio-format core/audio-format-mono-16 :name "ES8"}))

(defn setup [])

(defn update-state [state] state)

(defn draw-state [state] state)

(q/defsketch async-sound-1
  :title "async-sound-1"
  :size [600 600]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:no-bind-output]
  :middleware [m/fun-mode])
