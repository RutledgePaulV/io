(ns io.github.rutledgepaulv.io.encoding
  (:import (java.util Base64 HexFormat)))

(defn bytes->string ^String [bites]
  (String. ^"[B" bites "UTF-8"))

(defn bytes->base64 ^String [^"[B" bites]
  (bytes->string (.encode (Base64/getEncoder) bites)))

(defn bytes->base64-url ^String [^"[B" bites]
  (bytes->string (.encode (Base64/getUrlEncoder) bites)))

(defn bytes->hex ^String [^"[B" bites]
  (.formatHex (HexFormat/of) bites))

(defn string->bytes ^"[B" [string]
  (.getBytes ^String string "UTF-8"))

(defn base64->bytes ^"[B" [^String base64]
  (.decode (Base64/getDecoder) (string->bytes base64)))

(defn base64-url->bytes ^"[B" [^String base64]
  (.decode (Base64/getUrlDecoder) (string->bytes base64)))

(defn hex->bytes ^"[B" [^String hex]
  (.parseHex (HexFormat/of) hex))
