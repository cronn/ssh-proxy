#!/usr/bin/bash

# assuming that 'bintrayUser' and 'bintrayApiKey' is set in ~/.gradle/gradle.properties
./gradlew bintrayUpload
