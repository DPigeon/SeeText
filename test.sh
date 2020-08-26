#!/bin/bash

echo "------------------------------ Lint --------------------------------------------"
./gradlew lint

echo "------------------------------ Debug Build -------------------------------------------"
./gradlew assembleDebug -PversCode=1 -PversName=1.0.0

echo "------------------------------ Unit Tests --------------------------------------"
./gradlew test --stacktrace

echo "------------------------------ Instrumented Tests ------------------------------"
./gradlew connectedCheck --stacktrace

echo "------------------------------ Release Build -----------------------------------"
./gradlew assembleRelease -PversCode=1 -PversName=1.0.0

$SHELL