(ns io.github.rutledgepaulv.io.protocols
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream File InputStream OutputStream)
           (java.net URI URL)
           (java.nio.file Path)
           (java.util.zip ZipEntry)))


(defprotocol IntoPath
  (->path [x]
    "Converts argument into a java.nio.file.Path object."))

(defprotocol IntoFile
  (->file [x]
    "Converts argument into a java.io.File object."))

(defprotocol IntoByteArray
  (->bytes [x]
    "Converts argument into an array of bytes."))

(defprotocol IntoInputStream
  (->input-stream [x]
    "Converts argument into an input stream."))

(defprotocol IntoOutputStream
  (->output-stream [x]
    "Converts argument into an output stream."))

(defprotocol IntoData
  (->data [x]
    "Converts argument into plain clojure data."))

(extend-protocol IntoData
  nil
  (->data [x] x)
  Object
  (->data [x] x)
  ZipEntry
  (->data [x]
    {:name               (.getName x)
     :size               (.getSize x)
     :time               (.getTime x)
     :comment            (.getComment x)
     :directory          (.isDirectory x)
     :crc                (.getCrc x)
     :last-access-time   (.getLastAccessTime x)
     :last-modified-time (.getLastModifiedTime x)}))

(extend-protocol IntoInputStream
  nil
  (->input-stream [x] (InputStream/nullInputStream))
  InputStream
  (->input-stream [x] x)
  URL
  (->input-stream [x] (.openStream x))
  URI
  (->input-stream [x] (->input-stream (.toURL x)))
  File
  (->input-stream [x] (io/input-stream x))
  String
  (->input-stream [x] (->input-stream (->bytes x)))
  Path
  (->input-stream [x] (io/input-stream (.toFile x))))

(extend-protocol IntoInputStream
  (class (byte-array 0))
  (->input-stream [x] (ByteArrayInputStream. x)))

(extend-protocol IntoOutputStream
  nil
  (->output-stream [_] (OutputStream/nullOutputStream))
  OutputStream
  (->output-stream [x] x)
  File
  (->output-stream [x] (io/output-stream x))
  Path
  (->output-stream [x] (->output-stream (->file x))))

(extend-protocol IntoPath
  File
  (->path [x] (.toPath x))
  String
  (->path [x] (Path/of x (into-array String [])))
  URI
  (->path [x] (Path/of x))
  URL
  (->path [x] (->path (.toURI x)))
  Path
  (->path [x] x))

(extend-protocol IntoFile
  File
  (->file [x] x)
  String
  (->file [x] (io/file x))
  Path
  (->file [x] (.toFile x)))

(extend-protocol IntoByteArray
  File
  (->bytes [x] (->bytes (io/input-stream x)))
  String
  (->bytes [x] (.getBytes x "UTF-8"))
  Path
  (->bytes [x] (->bytes (->file x)))
  ByteArrayOutputStream
  (->bytes [x] (.toByteArray x))
  InputStream
  (->bytes [x]
    (let [out (ByteArrayOutputStream.)]
      (with-open [in x out out]
        (io/copy in out))
      (->bytes out))))