@echo off
REM Run tests locally with postgres_object_test schema
REM This script sets the required environment variable and runs the test suite

set POSTGRES_SCHEMA=postgres_object_test

echo Running tests with POSTGRES_SCHEMA=%POSTGRES_SCHEMA%
.\gradlew.bat test %*
