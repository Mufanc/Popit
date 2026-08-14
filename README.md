# Popit!

Popit! captures the foreground Android activity through the system Assistant API and reopens it as a notification bubble from a Quick Settings tile.

This is an experimental Android app for exploring `ROLE_ASSISTANT`, assist data, and notification bubbles. It uses Android Framework UI and APIs directly, with no AndroidX or third-party runtime libraries.

## Requirements

- Android 15 or later (API 35+)
- Popit! selected as the default assistant
- Notification permission
- Notification bubbles enabled for Popit!
- A launcher or System UI implementation that supports notification bubbles

## Setup

1. Open Popit!.
2. Select Popit! as the default assistant.
3. Allow notifications.
4. Enable notification bubbles.
5. Add the Popit! Quick Settings tile.

Selecting Popit! as the default assistant replaces the assistant currently configured on the device.

## Usage

1. Open the app or activity you want to capture.
2. Expand Quick Settings and tap the Popit! tile.
3. Popit! requests the foreground activity's assist data, returns to Home, and posts an auto-expanding bubble.

Only failure paths display a toast. A successful capture proceeds without an additional confirmation message.

Opening the same application again replaces its existing Popit! bubble. Different applications can have bubbles at the same time. Removing a bubble also removes its associated long-lived shortcut.

## Limitations

- The target activity must provide an `Intent` through assist data.
- The target activity must be exported so another application can launch it.
- Applications may omit, restrict, or customize assist data.
- The captured `Intent` reopens an activity; it does not transfer the original task or back stack into the bubble.
- Bubble auto-expansion is ultimately controlled by System UI and may be ignored.
- Behavior can vary between Android builds and device manufacturers.

## Build

The project uses AGP 9.2.1, Java 21, Kotlin through AGP's built-in Kotlin support, and the Gradle wrapper.

```shell
./gradlew assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it only for the primary Android user with:

```shell
adb install --user 0 -r app/build/outputs/apk/debug/app-debug.apk
```
