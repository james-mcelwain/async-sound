(ns cv.channel)

(defrecord Channel [!value])

(defn make-channel []
  (->Channel (atom 0)))

(defn set-value [channel value]
  (swap! (:!value channel) (constantly value)))

(defn get-value [channel]
  @(:!value channel))

(def c (make-channel))

(get-value c)
