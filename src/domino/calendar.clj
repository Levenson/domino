;; -*- mode: clojure; -*-

(ns domino.calendar
  (:use domino.notes
        domino.properties
        [domino.mime :exclude (*session* with-session)]
        clojure.java.io
        clojure.tools.logging
        [clojure.string :only (join blank?)]
        [clojure.pprint :only (cl-format)])
  (:require  [clj-ical.format :as ical] )
  (:import (lotus.domino Item MIMEEntity)
           (java.text SimpleDateFormat DateFormat)
           (javax.mail.internet  MimeMessage MimeMultipart
                                 PreencodedMimeBodyPart MimeBodyPart InternetAddress InternetHeaders)))

;; (defn document-to-ical [doc]
;;   (ical/write-object
;;    [:vcalendar
;;     [:x-lotus-charset "UTF-8"]
;;     [:version "2.0"]
;;     [:method "PUBLISH"]
;;     [:vevent
;;      [:description (.getItemValueString doc "body")]
;;      [:location (.getItemValueString doc "Location")]
;;      [:summary      (if (.hasItem doc "Topic")
;;                       (.getItemValueString doc "Topic")
;;                       (.getItemValueString doc "Subject"))]
;;      [:alarm (.getItemValueString doc "$Alarm")]
;;      [:alarmoffset (.getItemValueString doc "$AlarmOffset") ]
;;      [:requiredattendees (when (.hasItem doc "REQUIREDATTENDEES")
;;                            (-> doc (.getFirstItem "REQUIREDATTENDEES") .getText))]
;;      [:optionalattendees (when (.hasItem doc "OPTIONALATTENDEES")
;;                            (-> doc (.getFirstItem "OPTIONALATTENDEES") .getText))]
;;      [:uid  (.getItemValueString doc "$REF")]]]))


(defn get-lotus-server-date-format []
  (let [international (-> *session* .getInternational)]
    (let [sep (.getDateSep international)]
      (cond
       (.isDateDMY international) (clojure.string/join sep ["dd", "MM", "yyyy"])
       (.isDateYMD international) (clojure.string/join sep ["yyyy", "MM", "dd"])
       :else (clojure.string/join sep ["MM", "dd", "yyyy"]) ))))

(defn get-start-date [item]
  (println (.getValueDateTimeArray item)))

(defn toUTCString [date]
  (let [old_format "EEE MMM dd hh:mm:ss zzz yyyy"
        utc_format "yyyyMMdd'T'HHmmss'Z'"]
    (.format (doto (new SimpleDateFormat old_format)
               (.setTimeZone (java.util.TimeZone/getTimeZone "UTC"))
               (.parse (.toString date))
               (.applyPattern utc_format)) date)))

(defn get-apointment-type [doc]
  (case (-> doc (.getFirstItem "AppointmentType") .getValueInteger)
    0 :appointment 
    1 :anniversary 
    2 :all_day_event 
    3 :meeting 
    4 :reminder))

(defn get-alarm [doc]
  (when (and (.hasItem "$Alarm")
             (not (.hasItem "$AlarmDisabled"))
             (pos? (-> doc (.getFirstItem "$Alarm") .getValueInteger)))
    
    ))

(defn sample []
  (with-database  [(:notes.server *domino-properties*)
                   (:notes.mailbox *domino-properties*)]
    (.setConvertMIME *session* false)
    ;; (println (get-lotus-server-date-format))
    (doseq [doc (ncollection (.search *database* "SELECT @IsAvailable(CalendarDateTime)"))
            ;; (ncollection (.getView *database* "EnterpriseJIRA\\cal"))
            ]
      (if (= (.getItemValueString doc "$MessageID")
             "<OF5E0D7F75.59EC168B-ON44257BF2.002711F9-44257C1A.002020E1@LocalDomain>")
        (do
          [:vcalendar
           (if (.getFirstItem doc "OrgRepeat")
             (let [date-start (.getFirstItem doc "StartDateTime")
                   date-end (.getFirstItem doc "EndDateTime")]
               (when date-start
                 (doseq [[s, e] (map (fn [a b] (list a b))
                                     (.getValueDateTimeArray date-start)
                                     (.getValueDateTimeArray date-start))]
                   [:vevent
                    [:uid (.getUniversalID doc)]
                    [:last-modified (toUTCString (-> doc .getLastModified .toJavaDate))]
                    [:dtstart (toUTCString (.toJavaDate s))]
                    [:dtend (toUTCString (.toJavaDate e))]]))))]


          ;; (when (.hasItem doc "StartDateTime")
          ;;   (get-start-date (.getFirstItem doc "StartDateTime")))
          )))))





























;; (with-document-as-mime doc
;;   (with-message [message]
;;     (.setDebug domino.mime/*session* true)
;;     ;; (.setDebugOut domino.mime/*session* (java.io.PrintStream. (output-stream "/home/abralek/projects/domino/trace.log") true) )
;;     (if (= Item/TEXT (.getType (.getFirstItem doc "Body")))
;;       (message-build-text message doc)
;;       (message-build-mime message doc))
;;     (with-open [f (output-stream (str "/home/abralek/.maildir/tmp/" doc) :encoding "UTF-8")]
;;       (.writeTo message f)) ))

;; (doseq [doc (ncollection (.search *database* "SELECT @IsAvailable(CalendarDateTime)"))]
;;   (info doc (.getNoteID doc) "Subject:" (.getItemValueString doc "Subject") "Form:" (.getItemValueString doc "Form"))
;;   (if (.hasItem doc "$ICAL_ORIG_STREAM")
;;     (.getItemValueString doc "$ICAL_ORIG_STREAM") 
;;     (do
;;       ;; (println (.getItemValueString doc "Subject"))
;;       (println "==================================================")
;;       (document-to-ical doc)
;;       (println "=================================================="))))

;;     ;; (print doc.)
;;     ;; (with-document-as-mime doc
;;     ;;   (with-message [message]
;;     ;;     (if (= Item/TEXT (.getType (.getFirstItem doc "Body")))
;;     ;;       (message-build-text message doc)
;;     ;;       (message-build-mime message doc))
;;     ;;     (with-open [f (output-stream (str "/home/abralek/.maildir/cur/" doc) :encoding "UTF-8")]
;;     ;;       (.writeTo message f))))

;;     ))
