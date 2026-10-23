# Maintainer Release Guide

This document contains instructions for project maintainers on publishing new releases of
SheetStream, cutting version tags, and onboarding to Maven Central.

---

## 1. Automated GitHub Releases (Tag-Triggered)

SheetStream uses a GitHub Actions workflow in `.github/workflows/release.yml` that automatically
builds, tests, and publishes a GitHub Release whenever a Git tag is pushed.

### Step-by-Step Release Workflow

1. Ensure all code changes, tests, and documentation are committed and pushed to `main`:
   ```bash
   git status
   git push origin main
   ```

2. Update `CHANGELOG.md` with the new version and release notes under `## [X.Y.Z] - YYYY-MM-DD`.

3. Ensure `build.gradle.kts` has the matching version:
   ```kotlin
   version = "1.0.0"
   ```

4. Create an annotated Git tag:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   ```

5. Push the tag to GitHub:
   ```bash
   git push origin v1.0.0
   ```

6. What happens automatically:
    - The `.github/workflows/release.yml` workflow triggers on the tag.
    - It runs the test suite (`./gradlew check`).
    - It verifies the strict no-emoji policy.
    - It builds the release artifacts (`sheetstream-1.0.0.jar` and `sheetstream-1.0.0-sources.jar`).
    - It creates a GitHub Release titled `v1.0.0` with the JARs attached.
    - JitPack automatically builds and serves the dependency via
      `com.github.KingAs6583:SheetStream:1.0.0`.

---

## 2. Maven Central Deployment (Sonatype Central Portal)

If you own the domain `fidluc.com`, you can publish directly under the official namespace
`com.fidluc:sheetstream:1.0.0`.

### Step 1: Register on Sonatype Central Publisher Portal

1. Navigate to [Sonatype Central Publisher Portal](https://central.sonatype.com) and create an
   account.
2. In your profile or namespace settings, click **Add Namespace**.
3. Enter `com.fidluc`.

### Step 2: Domain Verification

1. Sonatype will provide a verification token (e.g. `central-verification=abcdef...`).
2. Log into your domain registrar/DNS provider for `fidluc.com`.
3. Add a new `TXT` record at the root of `fidluc.com` containing the verification token.
4. Return to the Sonatype Portal and click **Verify**. Once verified, the `com.fidluc` namespace is
   permanently unlocked.

### Step 3: GPG Key Generation & Key Distribution

Maven Central requires every uploaded artifact (`.jar`, `.pom`, `sources.jar`) to be digitally
signed with a GPG key.

1. Generate a new key pair:
   ```bash
   gpg --gen-key
   ```
2. List your keys to obtain the Key ID:
   ```bash
   gpg --list-keys --keyid-format SHORT
   ```
3. Publish your public key to the OpenPGP keyserver:
   ```bash
   gpg --keyserver keys.openpgp.org --send-keys <YOUR_KEY_ID>
   ```
4. Export your private key ring file (for Gradle signing):
   ```bash
   gpg --export-secret-keys -o secring.gpg
   ```

### Step 4: Sonatype User Token

1. In the Sonatype Central Portal, click on your username in the top right and select **View
   Account**.
2. Click **Generate User Token** to receive your `sonatypeUsername` and `sonatypePassword`.

### Step 5: Configure Gradle Secrets

Add the following to your local `~/.gradle/gradle.properties` (never commit this file to Git):

```properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=C:/path/to/secring.gpg

sonatypeUsername=YOUR_PORTAL_USER_TOKEN_USERNAME
sonatypePassword=YOUR_PORTAL_USER_TOKEN_PASSWORD
```

### Step 6: Publish

Run the Gradle publishing task:

```bash
./gradlew publish
```

The portal will validate your POM metadata, checksums, signatures, and Javadoc/Sources JARs, then
automatically synchronize the release across Maven Central.
