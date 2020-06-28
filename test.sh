#!/bin/bash

echo "------------------------------ Lint --------------------------------------------"
./gradlew lint

echo "------------------------------ Debug Build -------------------------------------------"
./gradlew assembleDebug

echo "------------------------------ Unit Tests --------------------------------------"
./gradlew test --stacktrace

echo "------------------------------ Instrumented Tests ------------------------------"
./gradlew connectedCheck --stacktrace

echo "------------------------------ Release Build -----------------------------------"
./gradlew assembleRelease

$SHELL