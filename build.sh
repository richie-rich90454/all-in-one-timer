#!/bin/sh
# Make it executable with chmod +x build.sh before running.
rm -f *.class TimerApp.jar
javac --release 8 AllInOneTimer.java
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi
jar cfe TimerApp.jar AllInOneTimer *.class *.ttf *.png
if [ -f TimerApp.jar ]; then
    echo "Build successful! Running application..."
    java -jar TimerApp.jar
else
    echo "JAR creation failed!"
    exit 1
fi