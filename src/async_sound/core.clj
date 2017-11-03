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


(def audio-format-4-16
  (let [sample-rate 44100
        sample-size 16
        channels 4
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

(defn listener [{name :name
                 audio-format :audio-format
                 min :min
                 max :max
                 chan-1 :chan-1
                 chan-2 :chan-2
                 chan-3 :chan-3
                 chan-4 :chan-4
                 frame-rate :frame-rate}]
  (fn []
    (async/thread
      (with-open [line (-> name mixer get-line (open-line audio-format))
                  out (java.io.ByteArrayOutputStream.)]
        (let [size (/ (.getBufferSize line) frame-rate)
              buffer (byte-array size)]
          ;; loop
          (loop []
            (do
              (.reset out)
              (when-let* [count     (did-read line buffer size)
                          ba        (.toByteArray (do (.write out buffer 0 size) out))
                          frames    (partition-all 4 (partition-all 2 ba))
                          {a :a
                           b :b
                           c :c
                           d :d}    (reduce (fn [{a :a b :b c :c d :d} [af bf cf df]]
                                              {:a (conj a af)
                                               :b (conj b bf)
                                               :c (conj c cf)
                                               :d (conj d df)})
                                            {:a [] :b [] :c [] :d []} frames)]
                (println chan-1 chan-2 chan-3 chan-4)
                (cond
                  (not (nil? chan-1)) (async/>!! chan-1 (average (reduce-frames a)))
                  (not (nil? chan-2)) (async/>!! chan-2 (average (reduce-frames b)))
                  (not (nil? chan-3)) (async/>!! chan-3 (average (reduce-frames c)))
                  (not (nil? chan-4)) (async/>!! chan-4 (average (reduce-frames d))))
                (recur)))))))))



(def chan-1 (async/chan (async/buffer 1)))
(def chan-2 (async/chan (async/buffer 1)))
(def chan-3 (async/chan (async/buffer 1)))
(def chan-4 (async/chan (async/buffer 1)))

(def listener (listener {:chan-1 chan-1 :chan-2 chan-2 :chan-3 chan-3 :chan-4 chan-4 :audio-format audio-format-4-16 :name "ES8" :frame-rate 30}))

(listener)

 (async/<!! chan-2)
