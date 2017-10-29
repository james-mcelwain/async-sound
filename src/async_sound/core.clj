(ns async-sound.core
  (:require
   [clojure.string :as str]
   [clojure.core.async :as async]))

(defn get-mixer-info-by-name [name]
  (filter #(str/includes? (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))

(defn get-line [mixer]
  (.getLine mixer (first (.getTargetLineInfo mixer))))

(defn mixer [name]
  (if-let [mixer-info (seq (get-mixer-info-by-name name))]
    (javax.sound.sampled.AudioSystem/getMixer (first mixer-info))
    (throw (Exception. (str "No mixer found with name " name)))))

(defn open-line [line audio-fmt]
  (do
    (.open line audio-fmt)
    line))

(defn start-line [line]
  (do
    (.start line)
    line))

(defn audio-format []
  (let [sample-rate 44100
        sample-size 16
        channels 1
        signed true
        big-endian false]
    (javax.sound.sampled.AudioFormat. sample-rate sample-size channels signed big-endian)))

(defn prepare-line [line]
  (-> line
      (open-line (audio-format))
      start-line))

(defn little-endian [b1 b2]
  (short (bit-or (bit-shift-left b1 8) (bit-and b2 0xFF))))

(defn listen [line]
  (let [size   (.getBufferSize line)
        out    (java.io.ByteArrayOutputStream.)
        buffer (byte-array size)]
      (if (.isOpen line)
        (do (.reset out)
            (let [count (.read line buffer 0 size)]
              (if (not (zero? count))
                (do (.write out buffer 0 count)
                    (let [ba (.toByteArray out)
                          sa (short-array (/ (.length ba) 2))]
                      (doseq [i (range (.length ba))
                              :when (even? i)]
                        (set sa (little-endian (get ba i) (get ba (inc i)))))))))))))


(listen (prepare-line (get-line (mixer "ES8"))))

