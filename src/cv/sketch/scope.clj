(ns cv.sketch.scope
  (:require [cv.core :as core]
             [quil.core :as q]
             [quil.middleware :as m]))

(defn setup []
  (q/background 0)
  (q/frame-rate 100)
  {:lx 0 :ly 0 :c (cycle (range (q/width))) :x 0 :y (/ (q/height) 2) })

(defn update-state [{:keys [x y c]}]
  (let [val @(:c0 core/channels)]
    (println val)
    {:lx x
     :ly y
     :gate true
     :c (rest c)
     :x (first c)
     :y (q/map-range val 30000 -30000 0 (q/height))}))

(defn draw-state [{:keys [gate x y lx ly]}]
  (q/stroke 0)
  (q/fill 0)
  (q/rect x 0 100 (q/height))

  (if (> (+ x 100) (q/width))
    (let [over (- (+ x 100) (q/width))]
      (q/rect 0 0 over (q/height))))

  (if (> x lx)
    (do
      (q/stroke-weight 1)

      (if gate
        (q/stroke 255 0 0 )
        (q/stroke 100))
      (q/line x y lx ly)))

  (q/fill 255)
  (q/ellipse x y 3 3))

(q/defsketch scope
  :title "scope"
  :size [1000 400]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:no-bind-output]
  :middleware [m/fun-mode])


