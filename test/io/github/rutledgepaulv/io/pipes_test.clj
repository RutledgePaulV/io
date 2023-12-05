(ns io.github.rutledgepaulv.io.pipes-test
  (:require [clojure.test :refer :all]
            [io.github.rutledgepaulv.io.pipes :as pipes]
            [io.github.rutledgepaulv.io.protocols :as protos]
            [io.github.rutledgepaulv.io.encoding :as encodings])
  (:import (java.io ByteArrayOutputStream)
           (java.util Arrays)))

(defn round-trips? [source & pipes]
  (let [sink (ByteArrayOutputStream.)]
    ((apply pipes/chain pipes) source sink)
    (Arrays/equals ^bytes (protos/->bytes source) ^bytes (.toByteArray sink))))

(defn output-str [source & pipes]
  (let [sink (ByteArrayOutputStream.)]
    ((apply pipes/chain pipes) source sink)
    (String. (.toByteArray sink) "UTF-8")))

(deftest round-trip-test
  (is (round-trips? "testing" pipes/gzip pipes/gunzip))
  (is (round-trips? "testing" pipes/deflate pipes/inflate))
  (is (round-trips? "testing" pipes/base64-encode pipes/base64-decode))
  (is (round-trips? "testing" pipes/base64-url-encode pipes/base64-url-decode)))

(deftest chaining-test
  (let [pipe (pipes/chain pipes/base64-encode pipes/gzip pipes/md5)
        [_ _ md5] (pipe "testing")]
    (is (= "7f1c229637fb7363e9acd3601efcb224" (encodings/bytes->hex md5)))))

(deftest hashing-test
  (is (= "ae2b1fca515949e5d54fb22b8ed95575"
         (encodings/bytes->hex (pipes/md5 "testing"))))
  (is (= "dc724af18fbdd4e59189f5fe768a5f8311527050"
         (encodings/bytes->hex (pipes/sha1 "testing"))))
  (is (= "cf80cd8aed482d5d1527d7dc72fceff84e6326592848447d2dc0b0e87dfc9a90"
         (encodings/bytes->hex (pipes/sha256 "testing"))))
  (is (= "521b9ccefbcd14d179e7a1bb877752870a6d620938b28a66a107eac6e6805b9d0989f45b5730508041aa5e710847d439ea74cd312c9355f1f2dae08d40e41d50"
         (encodings/bytes->hex (pipes/sha512 "testing")))))

(deftest grepping-test
  (is (= "testing\nmuffin\n" (output-str "testing\ndog\nmuffin\nthing" (pipes/grep #"test|muffin")))))

(deftest sed-test
  (is (= "sweeting\ndog\nsweet\nthing\n" (output-str "testing\ndog\nmuffin\nthing" (pipes/sed #"test|muffin" "sweet")))))

(deftest head-test
  (is (= "testing\ndog\nmuffin\n" (output-str "testing\ndog\nmuffin\nthing" (pipes/head 3))))
  (is (= "testing\ndog\nmuffin\nthing\n" (output-str "testing\ndog\nmuffin\nthing" (pipes/head 4))))
  (is (= "testing\ndog\nmuffin\nthing\n" (output-str "testing\ndog\nmuffin\nthing" (pipes/head 5)))))

(deftest sh-test
  (is (= "testing" (output-str "testing" (pipes/sh {} "cat")))))