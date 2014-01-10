;; -*- mode: clojure; -*-

(ns domino.notes
  (:use clojure.tools.logging domino.properties)
  (:import (lotus.domino NotesThread NotesFactory NotesException)
           (lotus.domino.local Session Document Item
                               MIMEEntity View ViewEntryCollection DocumentCollection)))

(def ^:dynamic *session*)
(def ^:dynamic *database*)

(def ^:dynamic *object*)

(defn notes-encondig
  [string]
  (case string
    nil 1725
    "quoted-printable" 1726
    "7bit" 1728
    "8bit" 1729
    "binary" 1730
    "base64" 1727))

(defn item-type
  [item]
  ;; (assert (= (type item) lotus.domino.local.Item) "Types mismatch")
  (when item
    (let [m (array-map
             16 :actioncd
             17 :assistantinfo
             1084 :attachment
             1076 :authors
             2 :collation
             1024 :datetimes
             1090 :embeddedobject
             256 :erroritem
             1536 :formula
             21 :html
             6 :icon
             20 :lsobject
             25 :mime_part
             1074 :names
             7 :notelinks
             4 :noterefs
             768 :numbers
             1085 :otherobject
             15 :querycd
             1075 :readers
             1282 :rfc822text
             1 :richtext
             8 :signature
             1280 :text
             512 :unavailable
             0 :unknown
             14 :userdata
             1792 :userid
             18 :viewmapdata
             19 :viewmaplayout
             )]
      (m (.getType item)))))


(defmacro with-notes-object
  [bindings & body]
  `(let ~(subvec bindings 0 2)
     (try
       (binding [*object* ~(bindings 0)]
         (trace "Builded" ~(bindings 0))
         (do ~@body))
       (finally
         (trace "Destroyed" ~(bindings 0))
         (.recycle ~(bindings 0))))))

(defmacro with-thread
  [& body]
  `(try
     (do
       (NotesThread/sinitThread)
       (do ~@body))
     ;; (catch Exception e)
     (finally (NotesThread/stermThread))))

(defmacro with-session
  [s & body]
  `(with-thread
     (let [pwd# (:notes.password *domino-properties*)]
       (with-notes-object
         [~s (if pwd# (NotesFactory/createSessionWithFullAccess pwd#) (NotesFactory/createSession))]
         (binding [*session* ~s]
           (info "Lotus Notes Version:" (.getNotesVersion ~s))
           (info "UserName:" (.getUserName ~s))
           (do ~@body))))))

(defmacro with-database
  "binding => [server mailbox]

Creates session to the specified server and open the database which
will be available through *database* variable in the scope of the
macro."
  [bindings & body]
  `(with-session s#
     (with-notes-object [d# (.getDatabase s# ~@(subvec bindings 0 2))]
       (binding [*database* d#]
         (info "Database Size:" (/ (.getSize d#) 1024 1024 ) "MB")
         (do ~@body)))))

(defmacro with-document
  [d & body]
  `(with-notes-object [d# ~d]
     (do ~@body)))

(defmacro with-document-validated
  "Returns document only if the document exists (is not a deletion stub) initially."
  [d & body]
  `(with-document ~d
     (when (.isValid ~d)
       (try
         (do ~@body)
         (finally (.markRead ~d))))))

(defmacro with-document-as-mime 
  "Converts NotesDocument into the MIME format."
  [d & body]
  `(try
     (do
       (.convertToMIME ~d Document/CVT_RT_TO_PLAINTEXT_AND_HTML 0)
       (.removeItem ~d "$KeepPrivate")
       (do ~@body))
     (catch NotesException e#
       (error ~d (.getNoteID ~d) "Subject:" (.getItemValueString ~d "Subject")))))

(defmacro with-mime-entity-transforming
  [binding & body]
  (assert (vector? binding))
  `(do
     (if (= "text" (.getContentType ~(first binding)))
       (.decodeContent ~(first binding))
       (.encodeContent ~(first binding) MIMEEntity/ENC_BASE64))
     (do ~@body)))

(defn get-first [coll]
  (condp = (type coll)
    ViewEntryCollection (.getFirstEntry coll)
    DocumentCollection (.getFirstDocument coll)
    View (.getFirstDocument coll)))

(defn get-next [coll d]
  (condp = (type coll)
    ViewEntryCollection (.getNextEntry coll d)
    DocumentCollection (.getNextDocument coll d)
    View (.getNextDocument coll d)))

;; collections
(defn nnext-document
  [coll d]
  ;; We take the next document here, because we have to release the
  ;; first one after processing it, but the the lazy-seq need to know
  ;; it to get the next one.
  (if-let [next-d (get-next coll d)]
    (cons d (lazy-seq
             (nnext-document coll next-d)))
    (list d)))

(defn ncollection
  [coll]
  (if-let [doc (get-first coll)]
    (nnext-document coll doc)
    nil))

(defn notes-all-seq
  [database]
  (ncollection (.getAllDocuments database)))

(defn notes-all-unread-seq
  [database]
  (let [coll (.getAllUnreadEntries (.getView *database* "($Inbox)"))]
    ;; (info "Found unread entries: " (.getCount coll))
    (ncollection coll)))

(defn mime-entity-seq
  [mime-entity]
  (loop [l [] c (.getFirstChildEntity mime-entity)]
    (if c
      (recur (conj l c) (.getNextSibling c))
      l)))

;;; maps
(defn headers-mime-map
  [m]
  (reduce #(conj %1 {(keyword (.getHeaderName %2))
                     (.getHeaderValAndParams %2 true true)})
          {}
          (.getHeaderObjects m)))

(defn headers-doc-map
  [d]
  (reduce (fn [coll h]
            (let [name (clojure.string/replace (.getName h) \$ \backspace)]
              (conj coll (case (item-type h)
                           :rfc822text {(keyword name) (.getText h)}
                           :datetimes  {(keyword name)
                                        (-> (java.text.SimpleDateFormat. "E, d M y k:m:s z")
                                            (.format (-> h .getDateTimeValue .toJavaDate)))}
                           :text (when (some #(= (clojure.string/lower-case name) %) ["from" "sendto" "subject"])
                                   {(keyword name) (.getText h)})
                           nil))))
          {}
          (.getItems d)))

(defn notes-headers-map
  [d]
  (cond
   (instance? MIMEEntity d) (headers-mime-map d)
   (instance? Document d) (headers-doc-map d)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; notes.clj ends here

