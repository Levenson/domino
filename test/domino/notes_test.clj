;; -*- mode: clojure; -*-

(ns domino.notes-test
  (:use clojure.test
        domino.export
        domino.properties
        domino.notes
        [domino.mime :exclude (*session* with-session)]
        clojure.java.io
        clojure.tools.logging
        [clojure.pprint :only (cl-format)])
  (:import (lotus.domino.local Item Document MIMEEntity)
           (javax.mail.internet  MimeMessage MimeMultipart
                                 PreencodedMimeBodyPart MimeBodyPart InternetAddress InternetHeaders)))

(deftest test1
  []
  (with-database [(:notes.server *domino-properties*)
                  (:notes.mailbox *domino-properties*)]
    ;; Read-write. Indicates whether items of type MIME_PART are
    ;; converted to rich text upon NotesDocument instantiation.
    (.setConvertMIME *session* false)
    (doseq [doc (ncollection (.getView *database* "test"))]
      ;; (is (= (type doc) Document))
      (binding [*out* *test-out*]
        (case (item-type (.getFirstItem doc "Body"))
          :mime_part (let [doc (.getMIMEEntity doc)
                           headers (notes-headers-map doc)]
                       (print headers)
                       ;; (is (= (.getContentType doc) (first (clojure.string/split (headers :Content-Type) #"/"))))
                       )
          "not implemented yet"))
      )))
