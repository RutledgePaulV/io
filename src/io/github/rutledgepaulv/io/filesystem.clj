(ns io.github.rutledgepaulv.io.filesystem
  (:require [clojure.java.io :as io]
            [clojure.string]
            [clojure.string :as strings]
            [io.github.rutledgepaulv.io.protocols :as protos]
            [io.github.rutledgepaulv.io.constants :as constants])
  (:import (clojure.lang IReduceInit Seqable)
           (java.io File)
           (java.nio.file FileSystems Files Path)))

(defn- prefix [] "io.filesystem")
(defn- suffix [] ".temp")

(defn relative
  "Returns a relative result from comparing the given path against the base path."
  [base path]
  (protos/fmap base (fn [base] (.normalize (.relativize ^Path (protos/->path base) ^Path (protos/->path path))))))

(defn absolute
  "Returns an absolute version of the provided argument with all indirection removed."
  ([path]
   (if (.exists (protos/->file path))
     (protos/fmap path (fn [x] (.toRealPath (protos/->path x) constants/LinkOptions0)))
     (protos/fmap path (fn [x] (.toAbsolutePath (.normalize (protos/->path x)))))))
  ([base path]
   (absolute (protos/fmap base (fn [base] (.normalize (.resolve ^Path (protos/->path base) ^String (protos/->string path))))))))

(defn new-temp-dir []
  (doto (.toFile (Files/createTempDirectory (prefix) constants/FileAttributes0))
    (.deleteOnExit)))

(defn new-temp-file
  ([]
   (doto (.toFile (Files/createTempFile (prefix) (suffix) constants/FileAttributes0))
     (.deleteOnExit)))
  ([dir]
   (doto (.toFile (Files/createTempFile (protos/->path dir) (prefix) (suffix) constants/FileAttributes0))
     (.deleteOnExit))))

(defn directory? [x]
  (let [file (protos/->file x)]
    (and (.exists file) (.isDirectory x))))

(defn file? [x]
  (let [file (protos/->file x)]
    (and (.exists file) (.isFile x))))

(defn executable? [x]
  (let [file ^File (protos/->file x)]
    (and (.exists file) (.isFile x) (.canExecute x))))

(defn which
  ([binary]
   (which (System/getenv "PATH") binary))
  ([path binary]
   (->> (strings/split (or path "") #":")
        (map io/file)
        (mapcat file-seq)
        (filter executable?)
        (reduce (fn [nf x] (if (= binary (.getName x)) (reduced (.getAbsolutePath x)) nf)) nil))))

(defn path-matcher [pattern]
  (let [matcher (.getPathMatcher
                  (FileSystems/getDefault)
                  (protos/->path-pattern pattern))]
    (fn [x] (.matches matcher (protos/->path x)))))

(defn glob-reducible
  ([glob]
   (glob-reducible (protos/->path ".") glob))
  ([root ^String glob]
   (reify
     Seqable
     (seq [this]
       (seq (persistent! (reduce (fn [agg x] (conj! agg x)) (transient []) this))))
     IReduceInit
     (reduce [this f init]
       (with-open [stream (Files/walk ^Path (protos/->path root) constants/FileOptions0)]
         (->> (iterator-seq (.iterator stream))
              (map (partial relative root))
              (filter (path-matcher glob))
              (reduce f init)))))))

(defn copy-dir [source target]
  )

(defn copy-file [source target]
  )

(defn move-file [source target]
  )

(defn delete-file-or-dir [x]
  )

(defn find-nearest-git-root [x]
  )
