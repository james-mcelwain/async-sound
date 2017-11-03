(ns async-sound.core
  (:require
   [clojure.string :as str]
   [clojure.core.async :as async]))

(defmacro when-let*
  ([bindings & body]
   (if (seq bindings)
     (do (println bindings)
       `(when-let [~(first bindings) ~(second bindings)]
          (when-let* ~(drop 2 bindings) ~@body)))
     `(do ~@body))))

(defn get-mixer-info-by-name [name]
  (filter #(str/includes? (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))

(defn get-line [mixer]
  (.getLine mixer (first (.getTargetLineInfo mixer))))

(defn mixer [name]
  (if-let [mixer-info (seq (get-mixer-info-by-name name))]
    (javax.sound.sampled.AudioSystem/getMixer (first mixer-info))
    (throw (Exception. (str "No mixer found with name " name)))))

(def audio-format-mono-16
  (let [sample-rate 44100
        sample-size 16
        channels 1
        signed true
        big-endian false]
    (javax.sound.sampled.AudioFormat. sample-rate sample-size channels signed big-endian)))

(defn open-line [line audio-format]
  (do
    (.open line audio-format)
    (.start line)
    line))

(defn little-endian [b1 b2]
  (short (bit-or (bit-and b1 0xFF) (bit-shift-left b2 8))))

(defn did-read [in buffer size]
  (let [count (.read in buffer 0 size)]
    (if (not (zero? count))
      count
      nil)))

(defn average [coll]
  (int (/ (reduce + coll) (count coll))))

(def channel (async/chan (async/sliding-buffer 1)))

(defn reduce-frames [frames] (reduce (fn [xs [lb hb]] (cons (little-endian lb hb) xs)) [] frames))


;; lib

(defn listener [{name :name audio-format :audio-format min :min max :max chan :chan frame-rate :frame-rate}]
  (fn []
    (async/thread
      (with-open [line (-> name mixer get-line (open-line audio-format))
                  out (java.io.ByteArrayOutputStream.)]
        (let [size (/ (.getBufferSize line) 1)
              buffer (byte-array size)]
          ;; loop
          (loop []
            (do
              (.reset out)
              (when-let* [count (did-read line buffer size)
                          ba (.toByteArray (do (.write out buffer 0 size) out))
                          frames (partition-all 2 ba)
                          avg (average (reduce-frames frames))]
                (println avg)
                (async/>!! chan avg)
                (recur)))))))))
