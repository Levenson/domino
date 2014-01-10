;; -*- mode: clojure; -*-

(ns domino.export-test
  (:use clojure.test
        domino.export
        domino.properties
        domino.notes
        [domino.mime :exclude (*session* with-session)]
        clojure.tools.logging)
  ;; (:import (lotus.domino Item Document MIMEEntity NotesException)
  ;;          (javax.mail.internet MimeMessage MimeMultipart
  ;;                                PreencodedMimeBodyPart MimeBodyPart InternetAddress InternetHeaders))
  )

(deftest test1
  []
  (binding [*out* *test-out*]
    (with-database [(:notes.server *domino-properties*)
                    (:notes.mailbox *domino-properties*)]
      (.setConvertMIME *session* false)
      (info "IsConvertMIME =" (.isConvertMIME *session*))
      (doseq [doc ;; (notes-all-seq *database*)
              (ncollection (.getView *database* "test2"))
              ]
        (warn "IN => " doc "subject:" (.getItemValueString doc "Subject"))
        (with-document-validated doc
          ;; (println (notes-headers-map doc))
          (to-file doc (str "/home/abralek/projects/domino/test/.maildir/new/" doc)))))))
