(ns cv.core
  (:require
   [clojure.string :as str]
   [clojure.core.async :as async]
   [cv.util :refer [when-let*]]
   [cv.gate :refer [gate]]
   [cv.cv :refer [cv]]
   [cv.format :as format]))

(defn get-mixer-info-by-name [name]
  (map #(println (.getName %)) (javax.sound.sampled.AudioSystem/getMixerInfo))
  (filter #(str/includes? (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))

(defn get-line [mixer]
  (.getLine mixer (first (.getTargetLineInfo mixer))))

(defn mixer [name]
  (if-let [mixer-info (seq (get-mixer-info-by-name name))]
    (javax.sound.sampled.AudioSystem/getMixer (first mixer-info))
    (throw (Exception. (str "No mixer found with name " name)))))

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

(defn reduce-frames [frames] (reduce (fn [xs [lb hb]] (cons (little-endian lb hb) xs)) [] frames))

;; lib
(defn handle-buffer-queue [[b chan handler]]
  (if (and (not (nil? handler)) (not (nil? chan)))
    (async/>!! chan (handler (reduce-frames b)))))

(def !listening (atom true))

(defn conj-frames [buffers frames]
  (map (fn [[buffer frame]] (conj buffer frame)) (partition 2 (interleave buffers frames))))

(defn listen [line out {:keys [name audio-format min max channels handlers frame-rate]}]
  ;; listen
  (let [size 512
        buffer (byte-array size)]
    (loop []
      (do
        (.reset out)
        (when-let* [start          (.toEpochMilli (java.time.Instant/now))
                    c              (did-read line buffer size)
                    ba             (.toByteArray (do (.write out buffer 0 size) out))
                    frames         (partition-all (count channels) (partition-all 2 ba))
                    buffers        (reduce conj-frames (take (count channels) (cycle [[]])) frames)
                    channel-groups  (partition 3 (interleave buffers channels handlers))]
          (run! handle-buffer-queue channel-groups)
      (if @!listening
        (recur)
        true))))))

(defn listener [opts]
  (fn []
    (async/thread
      (with-open [line (-> (:name opts) mixer get-line (open-line (:audio-format opts)))
                  out (java.io.ByteArrayOutputStream.)]
        (listen line out opts)))))

;; we always want the most recent sample
(defn channel [] (async/chan (async/sliding-buffer 1)))

(def c0 (channel))
(def c1 (channel))
(def c2 (channel))
(def c3 (channel))

(def ES8 (listener
               ;; channels
               {:channels [c0 c1 c2 c3]
                :handlers [cv cv cv cv]
                ;;
                :audio-format format/x4-96000-16bit
                ;; soundcard device name
                :name "ES-8"
                ;;
                :frame-rate 30}))
