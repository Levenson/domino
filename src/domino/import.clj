;; -*- mode: clojure; -*-

(ns domino.import
  (:use domino.notes
        clojure.java.io
        [clojure.pprint :only (cl-format)]
        [clojure.tools.logging :only (info error)])
  (:import (javax.mail.internet MimeMultipart InternetAddress MimeMessage)))

(defn header-seq [mail]
  (enumeration-seq (.getAllHeaders mail)))

(defn bodypart-seq [mime-multipart]
  (map #(.getBodyPart mime-multipart %1) (range (.getCount mime-multipart))))

(defn notes-headers
  [part mime]
  (doseq [h (filter #(not (= "X-Notes-Item" (.getName %1))) (header-seq part))]
    (-> mime (.createHeader (.getName h)) (.setHeaderValAndParams (.getValue h)))))

(defn notes-body
  [part mime]
  (notes-headers part mime)
  (with-open [s (.createStream *session*)]
    (.setContents s (reader (.getRawInputStream part)))
    (.setContentFromText mime s (.getContentType part) (notes-encondig (.getEncoding part)))))

(defn message-print
  [mail mime]
  ;; It will recycle object at the end.
  (with-notes-object [mime mime]
    (if (re-find #"multipart/*" (.getContentType mail))
      (do
        (notes-headers mail mime)
        (doseq [body (bodypart-seq (.getContent mail))]
          (cond
           (.isMimeType body "message/*") (message-print (.getContent body) (.createChildEntity mime))
           (.isMimeType body "multipart/*") (message-print body (.createChildEntity mime))
           :else (notes-body body (.createChildEntity mime)))))
      (notes-body mail (.createChildEntity mime)))))

(defn from-mail [mail]
  (with-notes-object [doc (.createDocument *database*)]
    (.replaceItemValue doc "Form" "Memo")
    (.setSaveMessageOnSend doc true)
    (info "OUT <==" doc (.getNoteID doc) "Subject:"  (.getSubject mail))
    (with-notes-object [mime (.createMIMEEntity doc)]
      (if (.isMimeType mail "multipart/*")
        (message-print mail mime)
        (notes-body mail mime))
      (.send doc))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; import.clj ends here
