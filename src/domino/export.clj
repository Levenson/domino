;; -*- mode: clojure; -*-

(ns domino.export
  (:use domino.properties
        domino.notes
        [domino.mime :exclude (*session* with-session)]
        [clojure.java.io :only [output-stream]]
        clojure.tools.logging))

(defn message-build-part
  [doc]
  (with-mime-entity-transforming [doc]
    (let [type  (.getContentType doc) sub (.getContentSubType doc)]
      (debug "MIME_PART"
             *parent* (.getContentType *parent*)
             "and mimeEntity is" (str type "/" sub) (.getCharset doc))
      ;; We have allready have Content-Type filed in `*parent*` javamail
      ;; mimepart, but *.setContent* will overwrite it. 
      (.setContent *parent* (.getContentAsText doc) (.getContentType *parent*)))))

(defn message-build-multipart
  [doc]
  (with-multipart [mpart {:subtype (.getContentSubType doc)}]
    (debug "MULTI_PART" mpart (str (.getContentType doc) "/" (.getContentSubType doc)))
    (doseq [e (mime-entity-seq doc)]
      ;; FIXIT with-notes-object
      (with-notes-object [e e]
        (let [type (.getContentType e) sub (.getContentSubType e)]
          (case type
            (or "multipart" "message")
            (with-bodypart [part (notes-headers-map e)]
              (message-build-multipart e))
            "text"
            (with-bodypart [part (notes-headers-map e)]
              (message-build-part e))
            (with-preencoded-bodypart [part (notes-headers-map e)]
              (message-build-part e))))))))

(defn message-mime
  [doc pathname]
  (let [mime (.getMIMEEntity doc)]
    (let [type (.getContentType mime) sub (.getContentSubType mime)]
      (with-message [message (notes-headers-map mime)]
        (case type
          "multipart" (message-build-multipart mime)
          "text" (message-build-part mime)
          (warn "Strange TYPE \"" (str type "/" sub) "\" of the document. Don't know what to do"))
        (with-open [f (output-stream pathname :encoding "UTF-8")]
          (.writeTo message f))))))

(defn message-text
  [doc pathname]
  (with-message [message]
    (headers-mime-add message (select-keys (notes-headers-map doc) [:Subject :From]))
    (.setContent message (-> doc (.getFirstItem "$Body_StoredForm") .getText) "text/plain")
    (with-open [f (output-stream pathname :encoding "UTF-8")]
      (.writeTo message f))))

(defn export-memo
  [doc pathname]
  (let [type (or (item-type (.getFirstItem doc "Body")))]
    (debug doc "body part is" (str type))
    (case type
      :mime_part (message-mime doc pathname)
      :richtext (with-document-as-mime doc
                  (debug "CONVERTED" doc (.getItemValueString doc "$MimeTrack"))
                  (message-mime doc pathname))
      :text  (message-text doc pathname)
      (warn "NOT IMPLEMENTED - IGNORE" (.getNoteID doc) "Subject:" (.getItemValueString doc "Subject")))))

(defn to-file
  [doc pathname]
  ;; Some times Document doesn't have Body 
  (case  (.getItemValueString doc "Form")
    "Memo" (export-memo doc pathname)
    "Notice" (with-document-as-mime doc
               (export-memo doc pathname))
    (warn doc "Form of the doc is:"  (.getItemValueString doc "Form") "don't know what to do!"
          "Subject:" (.getItemValueString doc "Subject"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; export.clj ends here

