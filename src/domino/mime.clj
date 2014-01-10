;; -*- mode: clojure; -*-

(ns domino.mime
  (:use clojure.tools.logging
        [domino.properties :only [read-prop]])
  (:import (javax.mail Session Message Address BodyPart MessagingException)
           (javax.mail.internet MimeMessage MimeMultipart PreencodedMimeBodyPart  MimeBodyPart InternetAddress InternetHeaders)))

(def ^:dynamic *session*)
(def ^:dynamic *properties*)
(def ^:dynamic *parent*)
(def ^:dynamic *part*)

(defmacro with-session
  "The Session class represents a mail session and is not
subclassed. It collects together properties and defaults used by the
mail API's. A single default session can be shared by multiple
applications on the desktop. Unshared sessions can also be created."
  [& body]
  `(let [properties# (read-prop "/javamail.properties")
         session# (Session/getInstance properties#)]
     (binding [*session* session#
               *properties* properties#]
       (do ~@body))))

(defmacro with-message
  "binding => [message headers]"
  [binding & body]
  (assert (vector? binding))
  `(with-session 
     ;; Returns subclass of MimeMessage for seting up personal
     ;; Message-ID header. Regarding this note
     ;; http://www.oracle.com/technetwork/java/faq-135477.html#msgid
     (let [message# (proxy [MimeMessage] [*session*]
                      (updateMessageID []))
           ~(first binding) message#
           headers# (or ~@(rest binding) nil)]
       (try
         (binding [*parent* message#]
           (headers-add *parent* headers#)
           (do ~@body))
         (finally
           (.saveChanges message#))))))

(defmacro with-part
  "binding => [instance headers]"
  [binding & body]
  `(let [part# ~(first binding) headers# (or ~@(rest binding) nil)]
     (try
       ;; (trace "CHILD" part# "of" *parent*)
       (binding [*parent* part#]
         ;; Javamail overwrite headers after filling the content, so
         ;; we will overwrite headers after.
         (headers-add *parent* headers#)
         (do ~@body))
       (finally
         (if (instance? MimeMultipart *parent*)
           (.addBodyPart *parent* part#)
           (.setContent *parent* part#))))))

(defmacro with-bodypart
  "binding => [var headers]"
  [binding & body]
  `(with-part [(new MimeBodyPart) (or ~@(rest binding) nil)]
     (let [~(first binding) *parent*]
       (do ~@body))))

(defmacro with-preencoded-bodypart
  "binding => [var headers]"
  [binding & body]
  `(with-part [(new PreencodedMimeBodyPart "base64") (or ~@(rest binding) nil)]
     (let [~(first binding) *parent*]
       (do ~@body))))

(defmacro with-multipart
  "binding => [var headers]"
  [binding & body]
  `(with-part [(new MimeMultipart)]
     (let [~(first binding) *parent* ;; headers# (or ~@(rest binding) nil)
           ]
       (.setSubType *parent* (get (or ~@(rest binding) {}) :subtype "mixed"))
       (do ~@body))))

(defn address [values]
  (into-array InternetAddress
              (mapv #(new InternetAddress %) (if (vector? values) values [values]))))

(def headers-handlers
  {:from (fn [o v] (.addFrom o (address v)))
   :to (fn [o v] (.addRecipients o javax.mail.Message$RecipientType/TO (address v)))
   :cc (fn [o v] (.addRecipients o javax.mail.Message$RecipientType/CC (address v)))
   :bcc (fn [o v](.addRecipients o javax.mail.Message$RecipientType/BCC (address v)))
   :subject (fn [o v]
              (assert (= (type v) String)) (.setSubject o v))})

(defn internet-headers [map]
  (let [headers (new InternetHeaders)]
    (doseq [[key value] map]
      (.addHeader headers (name key) (if (vector? value) (clojure.string/join ", "  value) value)))
    headers))

(defn content-type [m]
  (first (re-seq #"(\w+)/([^ ;]+);?" (.getContentType m))))


(defn internet-headers-seq
  [map]
  (enumeration-seq (.getAllHeaders (internet-headers map))))

(defn internet-header-lines-seq [map]
  (enumeration-seq (.getAllHeaderLines (internet-headers map))))

(defn headers-mime-add [part headers]
  (doseq [[k v] headers]
    (.addHeaderLine part (str (name k) ": " v))
    (trace "HEADER" (str (name k) ": " v) "added for" part)))

(defn headers-add [part headers]
  (if (= (type part) MimeMessage)
    (doseq [h (keys headers)]
      ((h headers-handlers) part (h headers)))
    (headers-mime-add part headers)))

;; (defn bodypart [headers content]
;;   (new MimeBodyPart (internet-headers headers) content))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; mime.clj ends here
