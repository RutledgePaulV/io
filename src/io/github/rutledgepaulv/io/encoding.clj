(ns io.github.rutledgepaulv.io.encoding
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [io.github.rutledgepaulv.io.protocols :as protos])
  (:import (clojure.lang IReduceInit LineNumberingPushbackReader)
           (java.io FilterInputStream)
           (java.util Base64 HexFormat)))

(defn bytes->hex [bites]
  (.formatHex (HexFormat/of) bites))

(defn bytes->base64 [bites]
  (String. (.encode (Base64/getEncoder) ^"[B" bites) "UTF-8"))

(defn hex->bytes [hex]
  (.parseHex (HexFormat/of) hex))

(defn base64->bytes [base64]
  (.decode (Base64/getDecoder) ^"[B" (.getBytes base64 "UTF-8")))

(defn zip-stream->reducing [source]
  (reify IReduceInit
    (reduce [this rf init]
      (with-open [stream (protos/->zip-input-stream source)]
        (loop [aggregate (unreduced init)]
          (if (reduced? aggregate)
            aggregate
            (if-some [entry (.getNextEntry stream)]
              (recur (rf aggregate
                         (assoc (protos/->data entry)
                           :stream (proxy [FilterInputStream] [stream]
                                     (close [])))))
              aggregate)))))))

(defn line-stream->reducing [stream]
  (reify IReduceInit
    (reduce [this f init]
      (with-open [reader (io/reader (protos/->input-stream stream))]
        (reduce f init (line-seq reader))))))

(defn edn-stream->reducing [stream]
  (let [edn-opts
        {:eof     ::eof
         :readers *data-readers*
         :default tagged-literal}]
    (reify IReduceInit
      (reduce [this f init]
        (with-open [reader (LineNumberingPushbackReader. (io/reader (protos/->input-stream stream)))]
          (loop [aggregate (unreduced init)]
            (if (reduced? aggregate)
              aggregate
              (let [entry (edn/read edn-opts reader)]
                (if (= ::eof entry)
                  aggregate
                  (recur (f aggregate entry)))))))))))