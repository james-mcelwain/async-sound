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
    (do
      (open-line line audio-format)
      (->Mixer name line out buf))
    (throw (Exception. (str "No mixer found with name " name)))))


(defn listen [mixer {:keys []}]

  )

(defn read->ba
  ([mixer]
   (read->ba mixer 512))
  ([mixer size]
   (let [line (:line mixer)
         out (:out mixer)
         buffer (:buf mixer)]
     (if (read line buffer size)
       (do
         (.write out buffer 0 size)
         (.toByteArray out))))))

(defn read->ba [mixer]

  )

(list-mixers)

(def es8 (make-mixer "ES-8" {:audio-rate cv.format/x4-44100-16bit}))
