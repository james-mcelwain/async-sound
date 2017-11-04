(ns cv.core
  (:require
   [clojure.string :as str]
   [clojure.core.async :as async]
   [cv.util :refer [when-let*]]
   [cv.gate :refer [gate]]
   [cv.cv :refer [cv]]
   [cv.format :as format]))

(defn get-mixer-info-by-name [name]
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

(defn listener [{:keys [name audio-format min max c0 c1 c2 c3 h0 h1 h2 h3 frame-rate]}]
  (fn []
    (async/thread
      (with-open [line (-> name mixer get-line (open-line audio-format))
                  out (java.io.ByteArrayOutputStream.)]
        (let [size (/ (.getBufferSize line) frame-rate)
              buffer (byte-array size)]

          ;; listen
          (loop []
            (do
              (.reset out)
              (when-let* [start     (.toEpochMilli (java.time.Instant/now))
                          count     (did-read line buffer size)
                          ba        (.toByteArray (do (.write out buffer 0 size) out))
                          frames    (partition-all 4 (partition-all 2 ba))
                          {:keys [a b c d]} (reduce
                                                 ;;               |                      |
                                                 ;;               | c0    c1    c2    c3 |
                                                 ;;               | f0    f1    f2    f3 |
                                             (fn [{:keys [a b c d]} [af    bf    cf    df]]
                                                      {:a (conj a af)
                                                       :b (conj b bf)
                                                       :c (conj c cf)
                                                       :d (conj d df)})
                                             {:a [] :b [] :c [] :d []} frames)]
                (handle-buffer-queue [a c0 h0])
                (handle-buffer-queue [b c1 h1])
                (handle-buffer-queue [c c2 h2])
                (handle-buffer-queue [d c3 h3])
                (if false (println (str count ":" (- (.toEpochMilli (java.time.Instant/now)) start) "ms")))))
            (recur)))))))

;; run

;; we always want the most recent sample
(defn ab [] (async/chan (async/sliding-buffer 1)))

(def c0 (ab))
(def c1 (ab))
(def c2 (ab))
(def c3 (ab))

(def ES8 (listener
               ;; channels
               {:c0 c0 :c1 c1 :c2 c2 :c3 c3
                ;; handlers
                :h0 cv :h1 cv :h2 cv :h3 cv
                ;;
                :audio-format format/c-4-16bit
                ;; soundcard device name
                :name "ES8"
                ;;
                :frame-rate 30}))

;; go

;; (loop []
;;   (println (clojure.string/join " " [(async/<!! c0) (async/<!! c1) (async/<!! c2) (async/<!! c3)]))
;;     (recur))
