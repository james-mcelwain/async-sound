(ns cv.core
  (:require
   [clojure.core.async :as async]
   [cv.gate :refer [gate]]
   [cv.cv :refer [cv]]
   [cv.util :refer [find-mixer-info]]
   [cv.format :as format])
  (:gen-class))

(defn- little-endian->int [[lb hb]]
  ;; for 16 bit audio, each sample comes in as two bytes
  (short (bit-or (bit-and lb 0xFF) (bit-shift-left hb 8))))

(defn- add-frame-to-bin [bins frame]
  ;; TODO: we probably want to use mutable java arrays, not seq due to performance
  ;; map with two collections iterates both seqs at once
  ;; given a frame (a b c d) and bins ([] [] [] []), we conj each data value
  ;; to the end of the list
  (map (fn [bin x] (conj bin x)) bins frame))

(defn- bin-by-channel [data channel-size]
  ;; partition (a b c d a b c d) into ((a b c d) (a b c d))
  ;; each sequence of (a b c d) constitutes a frame
  (let [frames (partition-all channel-size data)
        bins (take channel-size (cycle [[]]))]
    ;; given a lazy seq of frames of N size (channels), reduce into N bins
    (reduce add-frame-to-bin bins frames)))

(defn- update-channel-state [data channel]
  ;; TODO: if it's a gate, we probably want to keep all the data
  ;; until the next time it's consumer -- it could be valuable
  ;; know if a gate fired any time in the last animation frame
  (reset! (:!state channel) data))

(defn- create-channel [channel]
  ;; return a closure for consumers to call that wraps state atom and
  ;; applies mapper to retrieved value, or nil if no value
  ;; was updated since the last time the fn was called
  (fn []
    (let [!state (:!state channel)
          [val _] (swap-vals! !state (constantly nil))
          mapper (:mapper channel)]
      (if val
        (mapper val)
        nil))))

;; global state to kill a listening thread from repl
(def !running (atom true))
;; buffer size for reading from data-line
(def !read-size (atom (* 28 256)))

(defn- listener [name audio-format mappers]
  ;; the format of bytes read from a sound-card depends on the audio-format
  ;; used to open the data line.
  ;;
  ;; we use 16 bit mono because it's the most useful representation of cv.
  ;; two channels of mono 16 bit little endian audio data looks like:
  ;;
  ;; | ch 1. lo byte | ch 1. hi byte | ch 2. lo byte | ch 2. hi byte | ...
  ;;
  ;; our goal is to first convert the byte stream into a seq of integers,
  ;; and then bin each sample by channel. consumers can then use that data
  ;; to compute, e.g., the average cv value for that time period or whether
  ;; a gate was high or low.
  ;;
  ;; the buffer size for the line tends to be quite large and it is possible
  ;; to read 100-500ms worth of sound data per read. this can be determined by calling
  ;; `DataLine#getBufferSize`. i've tended to keep the amount read significantly smaller
  ;; since latency tends to matter more with cv. we only care about the current value of
  ;; a cv parameter, we can discard previous values unlike audio rate data.
  (println (str name " " audio-format))

  ;; get a line from our soundcard
  (let [mixer-info (find-mixer-info name)
        mixer (javax.sound.sampled.AudioSystem/getMixer mixer-info)
        line-info (first (.getTargetLineInfo mixer))
        line (.getLine mixer line-info)]

    ;; open and start the line
    (.open line audio-format)
    (.start line)

    ;; setup buffered io
    (let [size @!read-size ;;(.getBufferSize line)
          channel-size (.getChannels audio-format)
          channels (map #(do {:mapper % :!state (atom nil)}) mappers)
          buffer (byte-array size)
          out (java.io.ByteArrayOutputStream.)]

      (async/thread
        (while @!running
          (.reset out)

          (let [count (.read line buffer 0 size)]
            ;; if we read any data...
            (if (not (zero? count))
              (do (.write out buffer 0 count)
                  (let [raw-data (map little-endian->int (partition-all 2 (.toByteArray out)))
                        channel-data (bin-by-channel raw-data channel-size)]
                    (doall (map update-channel-state channel-data channels))))
              (println "WARNING: read 0 bytes from line"))))
        (println "---> exited"))
      (map create-channel channels))))

(defn get-channels [] (listener "ES9 [plughw:1,0]" cv.format/x14-96000-16bit [cv cv]))

(comment
  (swap! !running not)

  (map #(.getName %) (javax.sound.sampled.AudioSystem/getMixerInfo))
  (map cv.util/show-line-info (javax.sound.sampled.AudioSystem/getMixerInfo)))



(defn -main []
  (async/thread
    (println "starting...")
    (let [channels (get-channels)]
      (while @!running
        (Thread/sleep 100)
        (println (map #(%) channels)))))

  (while true))
