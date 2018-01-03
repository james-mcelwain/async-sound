(ns cv.midi
(:require [overtone.midi :as midi]
          [cv.core :as core]
          [clojure.core.async :as async]))

(defn midi-bus [{:keys [channels name]}]
  (println (str "open midi bus " name))
  (if-let [device (midi/midi-in name)]
    (do
      (println (str "openend " name))
      (midi/midi-handle-events
       device
       (fn [midi-msg]
         (println midi-msg)
         (if-let [chan (get channels (:note midi-msg))]
           (async/>!! chan (:velocity midi-msg))))))))

(def cc0 (core/channel))

(defn debug []
  (midi-bus {:channels [cc0] :name "iac"})
  (async/thread
    (loop []
      (println (async/<!! cc0))
      (recur))))

(debug)
