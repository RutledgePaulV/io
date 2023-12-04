## IO

An elegant library for working with IO in Clojure.

---

## Usage

### Pipes

Pipes are functions that take a source and a sink and may or may not return a result.

```clojure
(require '[io.github.rutledgepaulv.io.pipes :as pipes])

(def pipeline
  (pipes/chain 
    ; only match lines that contain the word "dog"
    (pipes/grep #"dog") 
    ; only take the first 5 matches
    (pipes/head 5)
    ; replace the word "dog" with "cat"
    (pipes/sed #"dog" "cat")
    ; compute a hash of the content before gzipping
    pipes/sha256
    ; gzip the data
    pipes/gzip))

(pipeline (io/file "input.txt") (io/file "output.txt"))
; (nil nil #object["[B" 0x49828df7 "[B@49828df7"] nil)
```

---

## Prior Art

- [babashka.fs](https://github.com/babashka/fs)
- [byte-streams](https://github.com/clj-commons/byte-streams)