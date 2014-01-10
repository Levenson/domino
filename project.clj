;; -*- mode: clojure; -*-

(defproject domino "0.2.0"
  :description "Proxy for Lotus Notes."
  :url "https://github.com/Levenson/domino"
  :bootclasspath true
  :repl-options {:host "127.0.0.1" :port 4545}
  :main domino.core
  :jvm-opts ["-Xmx1g" "-Xms1g"
             ;; "-XshowSettings:all"
             "-Xbootclasspath/p:.:/opt/ibm/notes/jvm/lib/ext/Notes.jar"
             ;; "-Xbootclasspath/p:.;C:\\Program Files (x86)\\Notes\\jvm\\lib\\ext\\Notes.jar"
             "-Djava.library.path=.:/opt/ibm/notes"
             ;; "-Djava.library.path=.;C:\\Program Files (x86)\\Notes"
             "-Dsun.boot.library.path=.:/opt/ibm/notes"]
             ;; "-Dsun.boot.library.path=.;C:\\Program Files (x86)\\Notes"
  :repositories [["java.net" "https://maven.java.net/content/repositories/releases"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.subethamail/subethasmtp "3.1.7"]
                 [javax.mail/mail "1.5.0-b01"]
                 [org.slf4j/slf4j-log4j12 "1.6.4"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ithayer/clj-ical "1.2"]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; project.clj ends here
