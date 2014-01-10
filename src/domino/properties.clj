
(ns domino.properties
  (:use clojure.tools.logging)
  (:require clojure.java.io))

(defn read-prop
  [name]
  (let [props (new java.util.Properties)]
    (if-let [f (-> Class (.getResourceAsStream name))]
      (with-open [^java.io.Reader reader (clojure.java.io/reader f)]
        (doto props
          (.load reader)))
      props)))

(def ^:dynamic *domino-properties*
  (into {} (for [[k v] (read-prop "/domino.properties")]
             (do
               (info k v)
               [(keyword k) (read-string v)]))))
