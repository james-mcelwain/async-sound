(ns async-sound.cv)

(defn average [coll]
  (int (/ (reduce + coll) (count coll))))

(defn cv [b]
  (average b))
