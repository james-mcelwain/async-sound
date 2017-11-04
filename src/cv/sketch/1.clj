(ns cv.sketch.1
  (:require [cv.core :as core]
            [clojure.core.async :as async]
            [quil.core :as q]
            [quil.middleware :as m]))

(def chan (async/chan (async/buffer 1)))

(defn map-sound [val val2]
  [(q/map-range val -30000 30000 0 255) (q/map-range val2 -30000 30000 0 255) (q/map-range val 30000 -30000 0 255) ])

(defn setup []
  (core/ES8)
  {:r 0 :g 0 :b 0})

(defn update-state [state]
  (let [[r g b] (map-sound (async/<!! core/c0) (async/<!! core/c1))]
    {:r r :g g :b b}))

(defn draw-state [{r :r g :g b :b}]
  (q/background r g b))

(q/defsketch cv-1
  :title "cv-1"
  :size [1000 600]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:no-bind-output]
  :middleware [m/fun-mode])
