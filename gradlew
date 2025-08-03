#!/bin/bash

# Simple Gradle wrapper script
GRADLE_APP_NAME="Gradle"
GRADLE_USER_HOME=${GRADLE_USER_HOME:-$HOME/.gradle}

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Check if Java is available
if ! command -v "$JAVACMD" &> /dev/null; then
    echo "ERROR: Java is not available in PATH and JAVA_HOME is not set"
    exit 1
fi

# Set the classpath to the wrapper jar
CLASSPATH="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"

# Execute Gradle
exec "$JAVACMD" -Xmx512m -Xms256m -Dorg.gradle.appname="$GRADLE_APP_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@" 