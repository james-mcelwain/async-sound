(ns gen-art.Sketch.lissa
  (:require
   [cv.core :as core]
   [cv.midi :as midi]
   [clojure.core.async :as async]
   [quil.core :as q]
   [quil.middleware :as m]))

;; CONSTANTS
(def  point-count       25)
(def  freq-x            3)
(def  freq-y            1)
(def  mod-freq-y        2)
(def  line-weight       0.3)
(def  line-color        20)
(def  line-alpha        20)
(def  connection-radius 400)

;; VARS
(def !phi         (atom 15))
(def !mod-freq-x  (atom 3))

(reset! !phi 21)
(reset! !mod-freq-x 1)

;; UTIL
(defn point [x y] [x y])

(defn debug [c0 c1 c2 c3]
  (println (str
            "c0: " (format "%-10s" c0)
            "c1: " (format "%-10s" c1)
            "c2: " (format "%-10s" c2)
            "c3: " (format "%-10s" c3))))

;; LIFECYCLE
(defn calc-angle [i]
  (q/map-range i 0 point-count 0 q/TWO-PI))

(defn calc-x [angle]
  (* (q/sin (+ (q/radians @!phi) (* angle freq-x))) (q/cos (* angle @!mod-freq-x))))

(defn calc-y [angle]
  (* (q/sin (* angle freq-y)) (q/cos (* angle mod-freq-y))))

(defn scale-point [p s]
  (int (* p (- (/ s 2) 30))))

(defn calc-points []
  (map #(let [angle (calc-angle %)
               x     (calc-x angle)
               y     (calc-y angle)]
           (point
            (scale-point x (q/width))
            (scale-point y (q/height)))) (range point-count))

  (for [i (range point-count)
        :let [angle (calc-angle i)
              x     (calc-x angle)
              y     (calc-y angle)]]
    (point
     (scale-point x (q/width))
     (scale-point y (q/height)))))

(def channels (cv.core/es8))

(defn map-cv [n]
  (q/map-range n -30000 30000 3 6))

(defn update-vars []

  ;; (map-cv ((nth channels 0)))
  ;; (map-cv ((nth channels 1)))
  (reset! !phi (q/map-range (q/mouse-x) 0 (q/width) 0 360))
  (reset! !mod-freq-x (q/map-range (q/mouse-y) 0 (q/height) 3 6)))

(defn setup []
  (q/frame-rate 30)
  ;; (midi/IAC)
  (calc-points))

(defn update-state [points]
  (update-vars)
  (calc-points))

(defn draw [points]
  (q/background 255)
  (q/color-mode :rgb)
  (q/stroke-weight line-weight)
  (q/stroke-cap :round)
  (q/no-fill)

  (q/push-matrix)
  (q/translate (/ (q/width) 2) (/ (q/height) 2))

  ;; LOOP
  (doseq [i1 (range point-count)
          i2 (range i1)
          :let [[x1 y1] (nth points i1)
                [x2 y2] (nth points i2)
                d       (q/dist x1 y1 x2 y2)
                a       (if (not (zero? d)) (q/pow (/ 1 (/ d (+ 1 connection-radius))) 2))]]

    (if (and (not (zero? d)) (< d connection-radius))
      (do
        (q/stroke line-color (* (int a) line-alpha))
        (q/line x1 y1 x2 y2))))

  (q/pop-matrix))

(q/defsketch gen-art
  :title "Lissajous"
  :size [800 800]
  :setup setup
  :update update-state
  :draw draw
  :features [:keep-on-top :no-bind-output]
  :middleware [m/fun-mode])



