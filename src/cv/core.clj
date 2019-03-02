(ns cv.core
  (:require
   [clojure.core.async :as async]
   [cv.util :refer [when-let*]]
   [cv.channel :as channel]
   [cv.gate :refer [gate]]
   [cv.mixer :as mixer]
   [cv.cv :refer [cv]]
   [cv.format :as format])
  (:gen-class))

(defn little-endian [b1 b2]
  (short (bit-or (bit-and b1 0xFF) (bit-shift-left b2 8))))

(def name "ES-8")

(defn conj-frames [buffers frames]
  (map (fn [[buffer frame]] (conj buffer frame)) (partition 2 (interleave buffers frames))))

(defn reduce-frames [frames]
  ;; [(-28, 58) (-25 58) ... ]
  (reduce (fn [xs [lb hb]] (cons (little-endian lb hb) xs)) [] frames))

(defn average [coll]
  (int (/ (reduce + coll) (count coll))))

(def !running (atom true))

(defn average [coll]
  (int (/ (reduce + coll) (count coll))))

(defn listener [name audio-format channels]
  ;; get a line from our soundcard
  (let [mixer-info (first  (filter #(= (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))
        mixer (javax.sound.sampled.AudioSystem/getMixer mixer-info)
        line-info (first (.getTargetLineInfo mixer))
        line (.getLine mixer line-info)]

    (println mixer-info mixer line-info line)

    ;; open and start the line
    (do
      (.open line audio-format)
      (.start line))

    ;; setup buffered io
    (let [size 512 ;;(.getBufferSize line)
          channel-size (.getChannels audio-format)
          buffer (byte-array size)]

      (async/thread
        (while @!running

          (Thread/sleep 1000)
          ;; read from the data line for our sound card.
          ;; the buffer size for the line tends to be quite large and it is possible
          ;; to read 100-500ms worth of sound data per read. this can be determined by calling
          ;; `DataLine#getBufferSize`. i've tended to keep the amount read significantly smaller
          ;; since latency tends to matter more with cv. we only care about the current value of
          ;; a cv parameter, we can discard previous values unlike audio rate data.
          (let [count (.read line buffer 0 size)]

            (println (seq buffer))
            ;; if we read any data...
            (if (not (zero? count))

              ;; the format of bytes read depends on the audio-format used to open the line.
              ;; we use 16 bit mono because it's the most useful representation of cv.
              ;; two channels of mono 16 bit little endian audio data looks like:
              ;;
              ;; | ch 1. lo byte | ch 1. hi byte | ch 2. lo byte | ch 2. hi byte | ...
              ;;
              ;; our goal is to group channel data together and deserialize the data
              ;; into 16 bit signed Java ints:
              ;;
              ;; (let [[channel-1 channel-2] [[] []]])
              ;;
              ;; these resulting vecs of ints can then be consumed, either to compute
              ;; and average cv value for the frame, or to test whether a gate was
              ;; high or not
              ;;
              ;; we try to do this with as few allocations as possible.
              ;;
              ;; TODO: currently, we're ignoring how much is read since getting
              ;; incomplete reads is pretty rare / incosequential, but we should
              ;; still handle this correctly.
              (let [frames (partition-all channel-size (partition-all 2 buffer))
                    buffers (reduce conj-frames (take channel-size (cycle [[]])) frames)
                    frames (map reduce-frames buffers)]
                (doall (map-indexed (fn [i val] (swap! ((keyword (str "c" i)) channels) (fn [_] (average val)))) frames)))
              (println "this shouldn't happen")
              )))
        (println "exited")
        ))))

;; (swap! !running not)

(def channels {:c0 (atom 0)
               :c1 (atom 0)
               :c2 (atom 0)
               :c3 (atom 0)})

(listener name cv.format/x4-44100-16bit channels)

@(:c0 channels)

(defn -main []
  (while true
    (Thread/sleep 1000)
    (println @(:c0 channels))))
