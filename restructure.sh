#!/bin/bash
set -e
P=app/src/main
J=$P/java/com/clipsync/app
mkdir -p $J .github/workflows
cp MainActivity.kt SyncService.kt SendActivity.kt Crypto.kt $J/
cp AndroidManifest.xml $P/AndroidManifest.xml
cp app_build.gradle app/build.gradle
cp build.yaml .github/workflows/build.yaml
