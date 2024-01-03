(ns io.github.rutledgepaulv.io.encoding
  (:import (java.util Base64 HexFormat)))

(defn bytes->string ^String [bites]
  (String. ^"[B" bites "UTF-8"))

(defn string->bytes ^"[B" [string]
  (.getBytes ^String string "UTF-8"))

(defn bytes->base64
  (^String [^"[B" bites]
   (bytes->base64 bites {}))
  (^String [^"[B" bites {:keys [url unpadded] :or {url false unpadded false}}]
   (-> (cond-> (if url (Base64/getUrlEncoder) (Base64/getEncoder))
         unpadded (.withoutPadding))
       (.encode bites)
       (bytes->string))))

(defn base64->bytes
  (^"[B" [^String base64]
   (base64->bytes base64 {}))
  (^"[B" [^String base64 {:keys [url] :or {url false}}]
   (-> (if url (Base64/getUrlDecoder) (Base64/getDecoder))
       (.decode (string->bytes base64)))))

(defn bytes->hex ^String [^"[B" bites]
  (.formatHex (HexFormat/of) bites))

(defn hex->bytes ^"[B" [^String hex]
  (.parseHex (HexFormat/of) hex))
