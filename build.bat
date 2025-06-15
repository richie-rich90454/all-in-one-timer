@echo off
setlocal
del *.class /Q
del TimerApp.jar /Q
javac -source 8 -target 8 AllInOneTimer.java
if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)
jar cfe TimerApp.jar AllInOneTimer *.class *.ttf *.png
if exist TimerApp.jar (
    echo Build successful! Running application...
    java -jar TimerApp.jar
) else (
    echo JAR creation failed!
)
endlocal