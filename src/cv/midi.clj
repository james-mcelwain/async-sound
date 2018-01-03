(ns cv.midi
  (:require [overtone.midi :as midi]
            [cv.core :as core]
            [clojure.core.async :as async]))

(defn event-handler [channels]
  (println "listening to " channels)
  (fn [midi-msg]
    (println midi-msg)
    ;; index of channel == cc number
    (if-let [chan (get channels (:note midi-msg))]
      (async/>!! chan (:velocity midi-msg)))))

(defn midi-bus [{:keys [channels name]}]
  (println (str "open midi bus " name))
  (if-let [device (midi/midi-in name)]
    (do
      (println (str "openend " name))
      (midi/midi-handle-events device (event-handler channels)))))

(def cc0 (core/channel))

(defn debug []
  (let [d (midi-bus {:channels [cc0] :name "iac"})]
    (async/thread
      (loop []
        (println (async/<!! cc0))
        (recur)))
    d))
