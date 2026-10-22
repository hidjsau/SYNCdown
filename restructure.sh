#!/bin/bash
set -e
P=app/src/main
J=$P/java/com/clipsync/app
mkdir -p $J $P/res/xml $P/res/values .github/workflows
cp *.kt $J/
cp AndroidManifest.xml $P/AndroidManifest.xml
cp accessibility_config.xml $P/res/xml/accessibility_config.xml
cp strings.xml $P/res/values/strings.xml
cp app_build.gradle app/build.gradle
cp build.yaml .github/workflows/build.yaml
