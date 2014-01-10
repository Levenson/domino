#!/usr/bin/env bash

NOTESBIN=/opt/ibm/notes

export CLASSPATH=${CLASSPATH}:.:${NOTESBIN}/jvm/lib/ext/Notes.jar

# This need for the library itself
export LD_LIBRARY_PATH=${NOTESBIN}
JAVA_OPTS="-Xmx1024m -Xms1024m -Xbootclasspath/p:$CLASSPATH"
# -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XshowSettings:all 
JAVA_OPTS="${JAVA_OPTS} -Djava.library.path=${LD_LIBRARY_PATH} -Dsun.boot.library.path=${LD_LIBRARY_PATH}"

export JAVA_OPTS

[[ -z ${JAVA_CMD} ]] && JAVA_CMD=java

${JAVA_CMD} -version

if [[ $1 == runuberjar ]]; then
  ${JAVA_CMD} ${JAVA_OPTS} -jar $(dirname ${0})/target/domino-0.2.0-standalone.jar
else
  lein $@
fi
