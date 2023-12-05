(ns io.github.rutledgepaulv.io.reducibles
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [io.github.rutledgepaulv.io.protocols :as protos])
  (:import (clojure.lang IReduceInit)
           (java.io FilterInputStream PushbackReader)
           (java.util.zip ZipInputStream)))


(defn zip-source->reducible [source]
  (reify IReduceInit
    (reduce [this rf init]
      (with-open [stream (ZipInputStream. (protos/->input-stream source))]
        (loop [aggregate (unreduced init)]
          (if (reduced? aggregate)
            (unreduced aggregate)
            (if-some [entry (.getNextEntry stream)]
              (recur (rf aggregate
                         (assoc (protos/->data entry)
                           :stream (proxy [FilterInputStream] [stream]
                                     (close [])))))
              aggregate)))))))

(defn line-delimited-source->reducible [source]
  (reify IReduceInit
    (reduce [this rf init]
      (with-open [reader (io/reader (protos/->input-stream source))]
        (reduce rf init (line-seq reader))))))

(defn edn-source->reducible
  ([source]
   (edn-source->reducible source {}))
  ([source opts]
   (let [edn-opts
         (merge
           {:readers *data-readers*
            :default tagged-literal}
           opts
           {:eof ::eof})]
     (reify IReduceInit
       (reduce [this rf init]
         (with-open [reader (PushbackReader. (io/reader (protos/->input-stream source)))]
           (loop [aggregate (unreduced init)]
             (if (reduced? aggregate)
               (unreduced aggregate)
               (let [entry (edn/read edn-opts reader)]
                 (if (= ::eof entry)
                   aggregate
                   (recur (rf aggregate entry))))))))))))