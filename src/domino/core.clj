
;; -*- mode: clojure; -*-

(ns domino.core
  (:use domino.properties
        domino.notes
        clojure.tools.logging)
  (:require clojure.repl domino.import domino.export)
  (:import javax.mail.internet.MimeMessage javax.mail.Session
           java.util.Properties
           org.subethamail.smtp.server.SMTPServer
           (org.subethamail.smtp.helper SimpleMessageListener SimpleMessageListenerAdapter))
  (:gen-class))

(def inbox (ref {}))

(defn message-store
  [from to message]
  (dosync (alter inbox assoc (keyword (.getMessageID message)) message)))

(defn message-delete
  [message]
  (dosync (alter inbox dissoc (keyword (.getMessageID message)))))

(defn message-listener
  []
  (proxy [SimpleMessageListener] []
    (accept [from to] true)
    (deliver [from to data]
      (message-store from to
                     (MimeMessage. (Session/getDefaultInstance (Properties.)) data)))))

(def ^:dynamic *smtp-server* (SMTPServer. (SimpleMessageListenerAdapter. (message-listener))))


(defn notes-to-mail
  []
  (doseq [doc (notes-all-unread-seq *database*)]
    (when (.isDocument doc)
      (let [doc (.getDocument doc)]
        (info "IN =>" doc "Subject:" (.getItemValueString doc "Subject"))
        (with-document-validated doc
          (domino.export/to-file doc
                                 (str (:domino.mailbox *domino-properties*) doc)))))))

(defn mail-to-notes
  []
  (doseq [message (vals @inbox)]
    (domino.import/from-mail message)
    (message-delete message)))

(defn shutdown
  [_]
  (info "Quitting.")
  (.stop *smtp-server*)
  (System/exit 0))

(defn run
  []
  (with-database [(:notes.server *domino-properties*)
                  (:notes.mailbox *domino-properties*)]
    ;; We have to set it here for successfully handling brakes by
    ;; NotesThread.
    (clojure.repl/set-break-handler! shutdown)
    ;; Read-write. Indicates whether items of type MIME_PART are
    ;; converted to rich text upon NotesDocument instantiation.
    (.setConvertMIME *session* false)
    (info "IsConvertMIME =" (.isConvertMIME *session*))
    (while true
      (mail-to-notes)
      (notes-to-mail)
      (Thread/sleep 10000))))

(defn -main
  []
  (doto *smtp-server*
    (.setPort (:domino.smtpd.port *domino-properties*))
    (.start))
  (run))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; core.clj ends here
