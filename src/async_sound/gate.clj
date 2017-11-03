(ns async-sound.gate)

(defn gate
  "if the signal is above ratio of the channel's max for n frames, we consider the gate high"
  ([buffer]
   (gate {:max 10} buffer 8 8/10))

  ([channel buffer]
   (gate channel buffer 8 8/10))

  ([channel buffer n]
   (gate channel buffer n 8/10))

  ([channel buffer n ratio]
  (let [threshold (* ratio (:max channel))
        length  (reduce (fn [xs x] (if (or (> xs n) (> x threshold)) (inc xs) 0)) 0 buffer)]
    {:gate (> length n) :length length})))



(gate '(10 10 10 10 10 10 10 10 10 10 0 0 0))
