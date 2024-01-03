(ns io.github.rutledgepaulv.io.filesystem
  (:require [io.github.rutledgepaulv.io.protocols :as protos])
  (:import (java.nio.file Files Path)
           (java.nio.file.attribute FileAttribute)))

(def FileAttributes0
  (into-array FileAttribute []))

(defn- prefix []
  "io.github.rutledgepaulv.io")

(defn- suffix []
  ".temp")

(defn resolve [base path]
  (.normalize (.resolve ^Path (protos/->path base) ^String path)))

(defn relative [base path]
  (.normalize (.relativize ^Path (protos/->path base) ^Path (protos/->path path))))

(defn new-temp-dir []
  (doto (.toFile (Files/createTempDirectory (prefix) FileAttributes0))
    (.deleteOnExit)))

(defn new-temp-file
  ([]
   (doto (.toFile (Files/createTempFile (prefix) (suffix) FileAttributes0))
     (.deleteOnExit)))
  ([dir]
   (doto (.toFile (Files/createTempFile (protos/->path dir) (prefix) (suffix) FileAttributes0))
     (.deleteOnExit))))

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
