domino
======

It is a Lotus Notes gateway allowing users to use any mail (e.g.,
Thunderbird) they want.


Usage
-----


You need to create your maildir and export all messages
there.

1. Create your mail directory (Maildir can be created by this
   command: mkdir -p ~/.maildir/{cur,new,tmp})
2. Set *mailbox* to ~/.maildir/cur directory
3. Set your values to domino.properties
4. Edit domino.sh. Add directory where Lotus Notes was installed into
   **LD\_LIBRARY\_PATH** variable. Add Notes.jar to your **CLASSPATH**, and I
   suggest you add Lotus Notes directory to **java.library.path** and
   **sun.boot.library.path**.
5. domino.sh run

It will run local smtpd on your 2500, which can be changed easily.


domino.properties
-----------------

```conf
notes.mailbox="<your notes mailbox>"
notes.server="<your notes server>"
notes.password="<your password (optional) >"
domino.mailbox="<your local directory for exporting email>"
domino.smtpd.port=2500
```

TODO
----

1. Need to build text/calendar mime part from "Appointment" or
   "Meeting" documents and attached it to the email.
