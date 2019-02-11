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

;; (defn read [in buffer size]
;;   (let [count (.read in buffer 0 size)]
;;     (if (not (zero? count))
;;       count
;;       nil)))


;; ;; lib
;; (defn handle-buffer-queue [[b chan handler]]
;;   (if (and (not (nil? handler)) (not (nil? chan)))
;;     (channel/set-value chan (handler (reduce-frames b)))))

;; ;; global suspend switch
;; (def !listening (atom true))

(defn conj-frames [buffers frames]
  (map (fn [[buffer frame]] (conj buffer frame)) (partition 2 (interleave buffers frames))))

;; (defn get-ms []
;;   (.toEpochMilli (java.time.Instant/now)))

;; (defn listen [line out {:keys [name audio-format min max channels handlers frame-rate]}]
;;   ;; listen
;;   (let [size 512
;;         buffer (byte-array size)]

;;     ;; hot loop
;;     (loop []
;;       (do
;;         (.reset out)
;;         (when-let* [start          (get-ms)
;;                     ba             (raw->ba line out buffer)
;;                     frames         (partition-all (count channels) (partition-all 2 ba))
;;                     buffers        (reduce conj-frames (take (count channels) (cycle [[]])) frames)
;;                     channel-groups (partition 3 (interleave buffers channels handlers))]
;;           (run! handle-buffer-queue channel-groups)
;;       (if @!listening
;;         (recur)
;;         true))))))

;; (defn listener [opts]
;;   (fn []
;;     (async/thread
;;       (with-open [mixer (mixer/make-mixer (:name opts) opts)
;;                   out (java.io.ByteArrayOutputStream.)]
;;         (println (str "Listening to " (:name opts)))
;;         (mixer/listen mixer)))))

;; ;; ----------------------------------------------------------------------------------------------

;; (def c0 (channel/make-channel))
;; (def c1 (channel/make-channel))
;; (def c2 (channel/make-channel))
;; (def c3 (channel/make-channel))

;; (def ES8 (listener
;;                ;; channels
;;                {:channels [c0 c1 c2 c3]
;;                 :handlers [cv gate cv cv]
;;                 ;;
;;                 :audio-format format/x4-96000-16bit
;;                 ;; soundcard device name
;;                 :name "ES-8"
;;                 ;;
;;                 :frame-rate 30}))

(def name "ES-8")

(defn conj-frames [buffers frames]
  (map (fn [[buffer frame]] (conj buffer frame)) (partition 2 (interleave buffers frames))))

(defn reduce-frames [frames] (reduce (fn [xs [lb hb]] (cons (little-endian lb hb) xs)) [] frames))

(defn average [coll]
  (int (/ (reduce + coll) (count coll))))

(defn -main []
  ;; get a line from our soundcard
  (let [mixer-info (first  (filter #(= (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))
        mixer (javax.sound.sampled.AudioSystem/getMixer mixer-info)
        line-info (first (.getTargetLineInfo mixer))
        line (.getLine mixer line-info)]

    ;; open the line
    (do
      (.open line cv.format/x4-44100-16bit)
      (.start line))

    (let [size 512
          buffer (byte-array size)
          out (java.io.ByteArrayOutputStream.)]

      (loop []
        ;; (.reset out)
        (let [count (.read line buffer 0 size)]
          (if (not (zero? count))
            ;; break into chunks of 2 and then 4 (channels)
            (let [frames (partition-all 4 (partition-all 2 buffer))
                  buffers (reduce conj-frames (take 4 (cycle [[]])) frames)
                  frames (map reduce-frames buffers)]
              (println (doall (map average frames)))
              (recur))))))))
