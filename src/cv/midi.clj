(ns cv.midi
  (:require [overtone.midi :as midi]
            [cv.core :as core]
            [clojure.core.async :as async]))

(defn event-handler [channels]
  (println "listening to " channels)
  (fn [midi-msg]
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

(defn vir-midi [] (midi-bus {:channels [cc0] :name "VirMIDI [hw:1,0,0]"}))

(def a (vir-midi))

(map :name )(midi/midi-devices)

(def MidiInDeviceInfo (nth (.getDeclaredClasses com.sun.media.sound.MidiInDeviceProvider) 0))

(defn- is-device-instance?
  [device] (.equals MidiInDeviceInfo (.getClass device)))

(defn get-devices [] (seq (javax.sound.midi.MidiSystem/getMidiDeviceInfo)))

(defn- get-device-info [name]
  (first (filter #( and (= (.getName %) name) (is-device-instance? %)) (get-devices))))

(defn get-device [name]
  (let [device-info (get-device-info name)]
    (javax.sound.midi.MidiSystem/getMidiDevice device-info)))

(defn- open-device [device]
  (if (not (.isOpen device))
    (.open device)))

(defn ->Reciever []
  (reify javax.sound.midi.Receiver
    (close [this] (println "Reciever was closed early."))
    (send [this msg time] (println msg))))

(defn- set-reciever [device]
  (.setReceiver (.getTransmitter device) (->Reciever)))


(def device (get-device "VirMIDI [hw:1,0,0]"))

(open-device device)
(set-reciever device)

(.getReceiver (.getTransmitter device))
