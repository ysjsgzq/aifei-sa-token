# Release Guide

This project is configured for publishing to Maven Central through the Sonatype Central Portal.

## Coordinates

```xml
<groupId>io.github.ysjsgzq</groupId>
<artifactId>aifei-sa-token</artifactId>
<version>1.0.0</version>
```

## Prerequisites

1. Own and verify the `io.github.ysjsgzq` namespace in Sonatype Central Portal.
2. Configure the Central token in `~/.m2/settings.xml` with server id `central`.
3. Install `gpg` locally.
4. Create or import a GPG secret key.
5. Make sure `gpg --list-secret-keys` can see the signing key.

## Publish command

```bash
mvn -Prelease clean deploy
```

The `release` profile will:

- sign artifacts with `maven-gpg-plugin`
- publish through `central-publishing-maven-plugin`
- wait until the release reaches the `published` state

## First-time GPG setup example

Install GPG:

```bash
brew install gnupg
```

Generate a key:

```bash
gpg --full-generate-key
```

List keys:

```bash
gpg --list-secret-keys --keyid-format LONG
```

If Maven cannot find the correct key automatically, you can publish with:

```bash
mvn -Prelease -Dgpg.keyname=YOUR_KEY_ID clean deploy
```
