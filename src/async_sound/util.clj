(ns async-sound.util)

(defn slurp-bytes
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defmacro when-let*
  ([bindings & body]
   (if (seq bindings)
     (do (println bindings)
         `(when-let [~(first bindings) ~(second bindings)]
            (when-let* ~(drop 2 bindings) ~@body)))
     `(do ~@body))))

