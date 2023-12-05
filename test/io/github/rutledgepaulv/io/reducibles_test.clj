(ns io.github.rutledgepaulv.io.reducibles-test
  (:require [clojure.test :refer :all]
            [io.github.rutledgepaulv.io.reducibles :as reducibles]
            [clojure.string :as strings]))

(deftest line-delimited-source->reducible-test
  (let [data   ["test" "one" "two" "three"]
        as-str (strings/join \newline data)]
    (is (= data (reduce conj [] (reducibles/line-delimited-source->reducible as-str))))))


(deftest edn-source->reducible-test
  (let [data   ["test" "one" "two" "three"]
        as-str (strings/join \space (map pr-str data))]
    (is (= data (reduce conj [] (reducibles/edn-source->reducible as-str))))))