(ns cv.mixer
  (:require [clojure.string :as str]))

(defn list-mixers []
  (let [mixer-info (javax.sound.sampled.AudioSystem/getMixerInfo)]
    (map #(.getName %) mixer-info)))

(defn get-mixer [name]
  (filter #(str/includes? (.getName %) name) (javax.sound.sampled.AudioSystem/getMixerInfo)))

(defn get-line [mixer]
  (.getLine mixer (first (.getTargetLineInfo mixer))))
 
(defn open-line [line audio-format]
  (do
    (.open line audio-format)
    (.start line)
    line))

(defn mixer [name]
  (if-let [mixer-info (seq (get-mixer name))]
    (javax.sound.sampled.AudioSystem/getMixer (first mixer-info))
    (throw (Exception. (str "No mixer found with name " name)))))

