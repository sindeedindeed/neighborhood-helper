#!/usr/bin/env bash

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/../" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

# Use the maximum available, or set MAX_FD != maximum.
MAX_FD="maximum"

warn ( ) {
    echo "$*"
}

die ( ) {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/bin/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --windows "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 3 -type d -name gradle 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_OPTS" != "" ] ; then
        GRADLE_OPTS="$GRADLE_OPTS \"-Dorg.gradle.appname=$APP_BASE_NAME\""
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a path
            arg=`cygpath --path --ignore --mixed --absolute "$arg"`
        fi
        i=$((i+1))
        eval "arg_${i}=\$arg"
    done
    case $i in
        (0) set -- ;;
        (1) set -- "$arg_1" ;;
        (2) set -- "$arg_1" "$arg_2" ;;
        (3) set -- "$arg_1" "$arg_2" "$arg_3" ;;
        (4) set -- "$arg_1" "$arg_2" "$arg_3" "$arg_4" ;;
        (5) set -- "$arg_1" "$arg_2" "$arg_3" "$arg_4" "$arg_5" ;;
        (6) set -- "$arg_1" "$arg_2" "$arg_3" "$arg_4" "$arg_5" "$arg_6" ;;
        (7) set -- "$arg_1" "$arg_2" "$arg_3" "$arg_4" "$arg_5" "$arg_6" "$arg_7" ;;
        (8) set -- "$arg_1" "$arg_2" "$arg_3" "$arg_4" "$arg_5" "$arg_6" "$arg_7" "$arg_8" ;;
        (9) set -- "$arg_1" "$arg_2" "$arg_3" "$arg_4" "$arg_5" "$arg_6" "$arg_7" "$arg_8" "$arg_9" ;;
        (*) set -- "${arg_1}" "${arg_2}" "${arg_3}" "${arg_4}" "${arg_5}" "${arg_6}" "${arg_7}" "${arg_8}" "${arg_9}" ;;
    esac
fi

# Escape application args
save ( ) {
    for arg do
        s/[\ \`\"$\\]/\\\\&/g
        s/^/-Xarg /
    done
    echo "$*"
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval "set -- $JAVA_OPTS $GRADLE_OPTS \"-Dorg.gradle.appname=$APP_BASE_NAME\" -classpath \"$CLASSPATH\" org.gradle.wrapper.GradleWrapperMain \"$@\""

# by default we should be in the correct project dir, but when run from Finder on Mac, the cwd is wrong
if [ "$(uname)" = "Darwin" ] && [ "$HOME" = "$PWD" ]; then
    cd "$(dirname "$0")"
fi

exec "$JAVACMD" "$@"

