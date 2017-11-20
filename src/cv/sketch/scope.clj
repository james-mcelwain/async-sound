(ns cv.sketch.scope
  (:require [cv.core :as core]
             [clojure.core.async :as async]
             [quil.core :as q]
             [quil.middleware :as m]))

(defn setup []
  (q/background 0)
  (q/frame-rate 100)
  {:lx 0 :ly 0 :c (cycle (range (q/width))) :x 0 :y (/ (q/height) 2) })

(def !missed (atom false))

(defn miss! [[val chan]]
  (swap! !missed (fn [_] (= chan :default)))
  val)

(defn update-state [{:keys [x y c]}]
  (let [c1 (async/<!! core/c1)
        val (miss! (async/alts!! [core/c0] :default y))]

    ;; log
    (println
     "c0:" (format "%-4s" x) (format "%-10s" y) (if @!missed " MISSED" "       ")
     "c1: " (str (:gate c1)) (:length c1) "len")

    {:lx x
     :ly y
     :c (rest c)
     :x (first c)
     :Y (q/map-range val 30000 -30000 0 (q/height))}))

(defn draw-state [{:keys [x y lx ly]}]
  (q/stroke 0)
  (q/fill 0)
  (q/rect x 0 100 (q/height))

  (if (> (+ x 100) (q/width))
    (let [over (- (+ x 100) (q/width))]
      (q/rect 0 0 over (q/height))))

  (if (> x lx)
    (do
      (q/stroke-weight 1)
      (q/stroke 100)
      (q/line x y lx ly)))

  (q/fill 255)
  (q/ellipse x y 3 3))

(async/<!! core/c0)

(q/defsketch scope
  :title "scope"
  :size [1000 400]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:no-bind-output]
  :middleware [m/fun-mode])


