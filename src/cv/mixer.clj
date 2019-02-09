(ns cv.mixer
  (:require [clojure.string :as str]
            [cv.util :refer [when-let*]]))

(defprotocol Closeable
  (close [this] "Closes the resource"))

(defrecord Mixer [name line out buf]
  Closeable
  (close [this] (.close line)))

(defn- list-mixers []
  (let [mixer-info (javax.sound.sampled.AudioSystem/getMixerInfo)]
    (map #(.getName %) mixer-info)))

(list-mixers)

(defn- get-mixer [name]
  (filter #(= (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))

(defn- get-line [mixer]
  (let [line-info (first (.getTargetLineInfo mixer))
        line (.getLine mixer line-info)]
    line))

(defn- open-line [line audio-format]
  (do
    (.open line audio-format)
    (.start line)
    line))

(defn make-mixer [name {:keys [audio-format]}]
  (when-let* [mixer-info (seq (get-mixer name))
              mixer (javax.sound.sampled.AudioSystem/getMixer (first mixer-info))
              line (get-line mixer)

              size 512
              buf (byte-array size)
              out (java.io.ByteArrayOutputStream.)]
    (->Mixer name line out buf)))

(defn open [mixer audio-format]
  (open-line (:line mixer) audio-format))

(defn formats [mixer]
  (map str (.getFormats (.getLineInfo (:line mixer)))))

(defn read->ba
  ([mixer]
   (read->ba mixer (count (:buf mixer))))
  ([mixer size]
   (let [line (:line mixer)
         out (:out mixer)
         buffer (:buf mixer)]
     (if (read line buffer size)
       (do
         (.write out buffer 0 size)
         (.toByteArray out))))))

(list-mixers)

(def es8 (make-mixer "ES-8" {:audio-format cv.format/x12-96000-24bit}))

(map println (formats es8))

(open es8 cv.format/x2-41000-16bit)
