(ns cv.debug
  (:require [cv.core :refer [c0 c1 c2 c3]]
            [clojure.core.async :as async]))

(defn debug []
  (async/thread
    (loop []
      (println (clojure.string/join " " (map async/<!! [c0 c1 c2 c3])))
      (recur))))
