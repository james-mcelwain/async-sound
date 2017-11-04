(ns cv.format)

(def mono-16bit
  (let [sample-rate 44100
        sample-size 16
        channels 1
        signed true
        big-endian false]
    (javax.sound.sampled.AudioFormat. sample-rate sample-size channels signed big-endian)))

(def 4chan-16bit
  (let [sample-rate 44100
        sample-size 16
        channels 4
        signed true
        big-endian false]
    (javax.sound.sampled.AudioFormat. sample-rate sample-size channels signed big-endian)))

