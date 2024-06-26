(ns io.github.rutledgepaulv.io.pipes
  (:require [clojure.java.io :as io]
            [clojure.java.process :as process]
            [clojure.string :as strings]
            [io.github.rutledgepaulv.io.protocols :as protos])
  (:import (java.io InputStream OutputStream PipedInputStream PipedOutputStream)
           (java.security DigestInputStream MessageDigest)
           (java.util Base64 Base64$Decoder Base64$Encoder Scanner)
           (java.util.zip Deflater DeflaterOutputStream GZIPInputStream GZIPOutputStream Inflater InflaterInputStream)))


(defn gzip
  ([] (gzip {}))
  ([{:keys [buffer-size sync-flush]
     :or   {buffer-size 4096 sync-flush false}}]
   (fn pipe [source sink]
     (with-open
       [in  (protos/->input-stream source)
        out (GZIPOutputStream. (protos/->output-stream sink) (int buffer-size) sync-flush)]
       (io/copy in out)))))

(defn gunzip
  ([] (gunzip {}))
  ([{:keys [buffer-size]
     :or   {buffer-size 4096}}]
   (fn pipe [source sink]
     (with-open
       [in  (GZIPInputStream. (protos/->input-stream source) (int buffer-size))
        out (protos/->output-stream sink)]
       (io/copy in out)))))

(defn deflate
  ([] (deflate {}))
  ([{:keys [buffer-size sync-flush]
     :or   {buffer-size 4096 sync-flush false}}]
   (fn pipe [source sink]
     (with-open
       [in  (protos/->input-stream source)
        out (DeflaterOutputStream. (protos/->output-stream sink) (Deflater.) (int buffer-size) sync-flush)]
       (io/copy in out)))))

(defn inflate
  ([] (inflate {}))
  ([{:keys [buffer-size]
     :or   {buffer-size 4096}}]
   (fn pipe [source sink]
     (with-open
       [in  (InflaterInputStream. (protos/->input-stream source) (Inflater.) (int buffer-size))
        out (protos/->output-stream sink)]
       (io/copy in out)))))

(defn digest [{:keys [algorithm]}]
  (fn pipe
    ([source]
     (pipe source nil))
    ([source sink]
     (let [digest (MessageDigest/getInstance algorithm)]
       (with-open [input-stream  (protos/->input-stream source)
                   digest-stream (DigestInputStream. input-stream digest)
                   output-stream (protos/->output-stream sink)]
         (io/copy digest-stream output-stream))
       (.digest digest)))))

(defn md5 []
  (digest {:algorithm "MD5"}))

(defn sha1 []
  (digest {:algorithm "SHA-1"}))

(defn sha256 []
  (digest {:algorithm "SHA-256"}))

(defn sha512 []
  (digest {:algorithm "SHA-512"}))

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

(defn sed [match replacement]
  (fn [source sink]
    (with-open [in  (protos/->input-stream source)
                out (protos/->output-stream sink)]
      (let [scanner (Scanner. ^InputStream in "UTF-8")]
        (while (.hasNextLine scanner)
          (let [line (strings/replace (.nextLine scanner) match replacement)]
            (.write ^OutputStream out (.getBytes line "UTF-8"))
            (.write ^OutputStream out (int \newline))))))))

(defn head [n]
  (fn [source sink]
    (with-open [in  (protos/->input-stream source)
                out (protos/->output-stream sink)]
      (let [scanner (Scanner. ^InputStream in "UTF-8")]
        (loop [i 0]
          (when (and (.hasNextLine scanner) (< i n))
            (let [line (.nextLine scanner)]
              (.write ^OutputStream out (.getBytes line "UTF-8"))
              (.write ^OutputStream out (int \newline))
              (recur (inc i)))))))))

(defn sh [{:keys [env dir]
           :or   {env {} dir "."}}
          & args]
  (fn [source sink]
    (let [proc (apply process/start
                      {:dir dir
                       :env (merge {} (System/getenv) env)}
                      args)]
      (future (with-open [in  (protos/->input-stream source)
                          out (:in proc)]
                (io/copy in out)))
      (with-open [in  (:out proc)
                  out (protos/->output-stream sink)]
        (io/copy in out)))))

(defn base64-encode
  ([] (base64-encode {}))
  ([{:keys [url unpadded]
     :or   {url false unpadded false}}]
   (let [encoder
         ^Base64$Encoder
         (cond-> (if url (Base64/getUrlEncoder) (Base64/getEncoder))
           unpadded (.withoutPadding))]
     (fn [source sink]
       (with-open [in  (protos/->input-stream source)
                   out (.wrap encoder (protos/->output-stream sink))]
         (io/copy in out))))))

(defn base64-decode
  ([] (base64-decode {}))
  ([{:keys [url] :or {url false}}]
   (let [decoder ^Base64$Decoder (if url (Base64/getUrlDecoder) (Base64/getDecoder))]
     (fn [source sink]
       (with-open [in  (.wrap decoder (protos/->input-stream source))
                   out (protos/->output-stream sink)]
         (io/copy in out))))))

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
                      (try
                        (with-open
                          [source (protos/->input-stream source)
                           sink   output]
                          (deliver p1 (pipe1 source sink)))
                        (catch Exception e
                          (deliver p1 e))))
                    input)]
          (try
            (deliver p2 (pipe2 source sink))
            (catch Exception e
              (deliver p2 e))))
        (flatten [(deref p1) (deref p2)])))))
  ([pipe1 pipe2 & pipes]
   (reduce chain (cons pipe1 (cons pipe2 pipes)))))