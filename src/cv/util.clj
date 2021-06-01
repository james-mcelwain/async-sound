(ns cv.util)

(defn find-mixer-info [name]
  (let [mixers (javax.sound.sampled.AudioSystem/getMixerInfo)]
    (first (filter #(= (.getName %) name) mixers))))

(defn show-line-info [line-info]
  (println line-info)
  (if (instance? javax.sound.sampled.DataLine$Info line-info)
    (let [data-line-info (cast javax.sound.sampled.DataLine$Info line-info)]
      (for [format (.getFormats data-line-info)]
        (println format)))))

(defn get-target-line-info [mixer]
  (let [mixer (javax.sound.sampled.AudioSystem/getMixer mixer)]
    (for [line (.getTargetLineInfo mixer)]
      (show-line-info line))))

