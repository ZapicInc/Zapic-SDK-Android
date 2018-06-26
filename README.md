# Zapic SDK for Android

[![Bintray](https://img.shields.io/bintray/v/zapic/maven/zapic-sdk-android.svg)](https://bintray.com/zapic/maven/zapic-sdk-android) ![Maven Central](https://img.shields.io/maven-central/v/com.zapic.sdk.android/zapic-sdk-android.svg) [![CodeFactor](https://www.codefactor.io/repository/github/zapicinc/zapic-sdk-android/badge)](https://www.codefactor.io/repository/github/zapicinc/zapic-sdk-android) [![MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Chat on Discord](https://img.shields.io/discord/430949891104309249.svg?logo=discord)](https://discord.gg/uC3k5D7) [![Twitter Follow](https://img.shields.io/twitter/follow/zapicinc.svg?style=social&label=Follow)](https://twitter.com/ZapicInc)

Copyright (c) 2017-2018 Zapic, Inc.

The Zapic SDK for Android is an open-source project that allows game developers to integrate with the Zapic platform from a game written in Kotlin or Java for Android. The Zapic SDK for Android supports devices running KitKat or higher (API 19).

_Android, Kotlin, Google Play, and the Google Play logo are trademarks of Google LLC._

_Oracle and Java are registered trademarks of Oracle and/or its affiliates._

## Getting Started

Learn more about integrating the SDK and configuring your Android game in the Zapic platform at https://www.zapic.com/docs/android.

## Community

Ask questions on [Stack Overflow](https://stackoverflow.com/questions/ask?tags=zapic). Be sure to include the `zapic` tag with your question.

Chat on [Discord](https://discord.gg/uC3k5D7).

Follow [@ZapicInc](https://twitter.com/ZapicInc) on Twitter for important announcements.

Report bugs and discuss new features on [GitHub](https://github.com/ZapicInc/Zapic-SDK-Android/issues).

## Contributing

We accept contributions to the Zapic SDK for Android. Simply fork the repository and submit a pull request on [GitHub](https://github.com/ZapicInc/Zapic-SDK-Android/pulls).

## Quick Links

* [Zapic Documentation](https://www.zapic.com/docs)
* [Zapic SDK for Unity](https://github.com/ZapicInc/Zapic-SDK-Unity)
* [Zapic SDK for iOS](https://github.com/ZapicInc/Zapic-SDK-iOS)

# Zapic Android Demo

This project includes a [Zapic Android demo game](zapic-demo) configured with the Zapic SDK for Android. Zapic is initialized with the demo game by attaching a fragment to the main activity during the `onCreate` lifecycle event. See [line 105 in MainActivity.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainActivity.java#L105).

The demo game is very simple. The activity counts the number of pixels as a player drags their finger across the background image. After the player drags their finger, the activity submits a gameplay event with a `DISTANCE` parameter that includes the total number of pixels. See [lines 211-215 in MainActivity.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainActivity.java#L211-L215).

The activity includes a Zapic branded button (required by the [Terms of Use](https://www.zapic.com/terms/)) that opens Zapic and shows the default page. See [line 84 in MainActivity.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainActivity.java#L84).

The activity also includes a "Challenges" button that explicitly opens the challenges page for the current game. See [line 92 in MainActivity.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainActivity.java#L92).

*Try it out! Get it on Google Play, make a friend on Zapic, and challenge your friend to collect the most number of pixels!*

[<img alt="Get it on Google Play" height="100" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />](https://play.google.com/store/apps/details?id=com.zapic.androiddemo)

<img alt="Zapic Android demo game screenshot" src="docs/screenshot_game.jpg" /> <img alt="Zapic challenge screenshot" src="docs/screenshot_challenge.jpg" />

## Deep Links

The demo game has been configured to use the [Branch](https://branch.io/) third-party integration to provide deep links.

The deep links feature allows a player to share very specific content with friends, such as challenge invitations. The game is automatically launched when another player opens the deep link. The deep links feature also redirects players to the Play Store to download the game if it is not already installed.

The Branch SDK has been configured to relay deep links to Zapic. If a deep link references actionable Zapic content, such as a challenge invitiation, then the Zapic activity automatically launches with a specially tailored user interface. If a deep link does not reference actionable Zapic content, it is simply ignored. See [lines 168-172 in MainActivity.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainActivity.java#L168-L172).

The Zapic SDK has also been configured to relay the unique user identifier to Branch. This enables user attribution within the Branch dashboard. See [lines 41-59 in MainApplication.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainApplication.java#L41-L59).

*Try it out! Open the link below on an Android device! The challenges page opens if the game is already installed. The Play Store opens if the game is not already installed.*

[**Open the Zapic Android Demo (only configured to work on an Android device)**](https://6k50.app.link/DLH7j8jpeM)

*Try it out! Ask a friend to to challenge you and to share the unique challenge link via SMS, Twitter, etc.!*

<img alt="Zapic challenge share screenshot" src="docs/screenshot_challenge_share.jpg" /> <img alt="Zapic challenge invite screenshot" src="docs/screenshot_challenge_invite.jpg" />

## Push Notifications

The demo game has been configured to use the [OneSignal](https://onesignal.com) third-party integration to provide push notifications.

The push notifications feature allows players to receive notifications as important events occur, such as challenge invitations, and to receive notifications that encourage re-engagement after a period of inactivity.

The OneSignal SDK has been configured to relay push notifications to Zapic. If a push notification references actionable Zapic content, such as a challenge invitiation, then the Zapic activity automatically launches with a specially tailored user interface. If a push notification does not reference actionable Zapic content, it is simply ignored. See [lines 31-34 in MainApplication.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainApplication.java#L31-L34).

The Zapic SDK has also been configured to relay the user's unique push notification token to OneSignal. Zapic uses the unique push notification token, *not the user identifier*, when sending content to users. This helps prevent users from impersonating one another (refer to the [OneSignal documentation](https://documentation.onesignal.com/docs/identity-verification) for more context). See [lines 41-59 in MainApplication.java](zapic-demo/src/main/java/com/zapic/androiddemo/MainApplication.java#L41-L59).

*Try it out! Ask a friend to challenge you! The push notification should be delivered shortly after the challenge has been created.*

<img alt="Zapic challenge notification screenshot" src="docs/screenshot_notification.jpg" /> <img alt="Zapic challenge invite screenshot" src="docs/screenshot_challenge_invite.jpg" />

## Building from Source

The demo application and library are built from source using Android Studio 3.1 or higher and the latest version of the Android SDK.

### Releasing the Library

First, bump the library module's `versionCode` and `versionName` in `build.gradle`.

Second, build and upload the library artifacts to BinTray.

On Linux/Mac OS, execute the following:

```sh
$ export BINTRAY_USER=XXXXXXXX
$ export BINTRAY_API_KEY=XXXXXXXX
$ export BINTRAY_GPG_PASSPHRASE=XXXXXXXX
$ ./gradlew clean zapic:assembleRelease zapic:bintrayUpload
```

On Windows, execute the following:

```sh
> set BINTRAY_USER=XXXXXXXX
> set BINTRAY_API_KEY=XXXXXXXX
> set BINTRAY_GPG_PASSPHRASE=XXXXXXXX
> .\gradlew.bat clean zapic:assembleRelease zapic:bintrayUpload
```

Third, create a new tag in the Git repository. Create a new release on GitHub with the changelog and AAR artifact.
