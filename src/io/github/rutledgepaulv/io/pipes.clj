(ns io.github.rutledgepaulv.io.pipes
  (:require [clojure.java.io :as io]
            [io.github.rutledgepaulv.io.protocols :as protos]
            [clojure.java.shell :as shell])
  (:import (java.io InputStream OutputStream PipedInputStream PipedOutputStream)
           (java.security DigestInputStream MessageDigest)
           (java.util Scanner)
           (java.util.zip DeflaterOutputStream GZIPInputStream GZIPOutputStream InflaterInputStream)))


(defn gzip [source sink]
  (with-open
    [in  (protos/->input-stream source)
     out (GZIPOutputStream. (protos/->output-stream sink))]
    (io/copy in out)))

(defn gunzip [source sink]
  (with-open
    [in  (GZIPInputStream. (protos/->input-stream source))
     out (protos/->output-stream sink)]
    (io/copy in out)))

(defn deflate [source sink]
  (with-open
    [in  (protos/->input-stream source)
     out (DeflaterOutputStream. (protos/->output-stream sink))]
    (io/copy in out)))

(defn inflate [source sink]
  (with-open
    [in  (InflaterInputStream. (protos/->input-stream source))
     out (protos/->output-stream sink)]
    (io/copy in out)))

(defn make-digest-pipe [algorithm]
  (fn [source sink]
    (let [digest (MessageDigest/getInstance algorithm)]
      (with-open [input-stream  (protos/->input-stream source)
                  digest-stream (DigestInputStream. input-stream digest)
                  output-stream (protos/->output-stream sink)]
        (io/copy digest-stream output-stream))
      (.digest digest))))

(defn md5
  ([source]
   (md5 source nil))
  ([source sink]
   ((make-digest-pipe "MD5") source sink)))

(defn sha1
  ([source]
   (sha1 source nil))
  ([source sink]
   ((make-digest-pipe "SHA-1") source sink)))

(defn sha256
  ([source]
   (sha256 source nil))
  ([source sink]
   ((make-digest-pipe "SHA-256") source sink)))

(defn sha512
  ([source]
   (sha512 source nil))
  ([source sink]
   ((make-digest-pipe "SHA-512") source sink)))

(defn grep [pattern]
  (fn [source sink]
    (with-open [in  (protos/->input-stream source)
                out (protos/->output-stream sink)]
      (let [scanner (Scanner. ^InputStream in "UTF-8")]
        (while (.hasNextLine scanner)
          (let [line (.nextLine scanner)]
            (when (re-find pattern line)
              (.write ^OutputStream out (.getBytes line "UTF-8"))
              (.write ^OutputStream out (int \newline)))))))))


(defn sh [command]
  (fn [source sink]
    (with-open [out (protos/->output-stream sink)]
      (let [process (shell/sh command)
            in (.getInputStream process)]
        (io/copy in out)))))

(defn chain
  ([pipe1]
   (fn [source sink]
     (flatten [(pipe1 source sink)])))
  ([pipe1 pipe2]
   (fn combo
     ([source]
      (combo source nil))
     ([source sink]
      (let [p1 (promise)
            p2 (promise)]
        (with-open
          [sink   (protos/->output-stream sink)
           source (let [input  (PipedInputStream.)
                        output (PipedOutputStream.)]
                    (.connect input output)
                    (future
                      (with-open
                        [source (protos/->input-stream source)
                         sink   output]
                        (deliver p1 (pipe1 source sink))))
                    input)]
          (deliver p2 (pipe2 source sink)))
        (flatten [(deref p1) (deref p2)])))))
  ([pipe1 pipe2 & pipes]
   (reduce chain (cons pipe1 (cons pipe2 pipes)))))