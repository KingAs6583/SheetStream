---
name: release-publish
description: >-
  Automates the release verification, version bumping, and publishing workflow for SheetStream.
  Use when preparing a release tag, checking pre-release requirements, or publishing to Maven Central or JitPack.
---

# Release and Publishing Runbook for SheetStream

Follow this workflow when preparing and releasing a new version of SheetStream.

## 1. Pre-Flight Verification

Ensure the repository adheres to all architectural invariants:
1. No emojis in any modified files, commit messages, or documentation.
2. Confirm Java 8 bytecode compatibility in `build.gradle.kts`:
   - `jvmTarget.set(JvmTarget.JVM_1_8)`
3. Ensure zero third-party runtime dependencies are added.

## 2. Run Test Suite

Run all automated unit tests and benchmark validations:
```bash
./gradlew test
```
Verify that all tests pass without errors or warnings.

## 3. Version Bump and Changelog

1. Update the version string in `build.gradle.kts`:
   ```kotlin
   version = "<new-version>"
   ```
2. Update `CHANGELOG.md`:
   - Move unreleased changes to a new section `## [<new-version>] - YYYY-MM-DD`.
   - Update version comparison links if applicable.

## 4. Build Artifacts

Generate the library JAR and sources JAR:
```bash
./gradlew assemble
```
Verify that the generated JAR contains valid class files targeting Java 8 bytecode.

## 5. Publishing

For Maven Central / local Maven repository:
```bash
./gradlew publishToMavenLocal
```
For Git tags (consumed by JitPack):
```bash
git add .
git commit -m "Release v<new-version>"
git tag v<new-version>
git push origin main --tags
```
