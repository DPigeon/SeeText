#!/bin/bash

echo "------------------------------ Lint ------------------------------"
./gradlew lint

echo "------------------------------ Unit Tests ------------------------------"
./gradlew test --stacktrace

echo "------------------------------ Instrumented Tests ------------------------------"
./gradlew connectedCheck --stacktrace

$SHELL