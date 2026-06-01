# Quick Settings Sound Profile

An app that lets you change your sound profile (ring, vibrate, mute) via a Quick Settings tile.  
The app is written 100% in Kotlin :D

## Demo video
<img src="assets/animation/demo_video.gif" width="486" height="1080" alt="Demo Animation"/>

## Features

While this app supports devices running Android 7 and above, later OS versions unlock a broader range of functionalities.

### Android 15 and Higher
On these devices, you can configure audible and visual settings individually for each **mode** in the Android system settings. This is possible because the so-called **ZenPolicy** in Android 15 offers significantly more granular control than in previous versions. While the app provides a generic ZenPolicy, you currently need to manually refine these rules in your device's system settings.

### Android 10 to 14
For devices running versions 10 through 14, the app automatically applies a default ZenPolicy whenever the phone is set to **Silent** mode. Currently, this policy only allows media sounds and alarms to go through. You can see this in the **Do Not Disturb** settings when the silent mode is active and the **Schedule** is triggered.

### Android 7 to 9
On older versions (Android 7 to 9), the app acts as a legacy bridge. It lets you toggle your ringer mode through the Quick Settings, but since these versions lack the **ZenPolicy** feature, you can't fine-tune the Do Not Disturb behavior as deeply as on newer devices.

## Why use an AutomaticZenRule?

When activating Silent Mode via the app's Quick Settings tile, a small visual discrepancy occurs: the status bar shows a "silent" icon, but pressing the physical volume buttons reveals that the system volume panel still displays the **vibrate** icon.
This behavior is an intentional workaround for a restrictive Android limitation.

In theory, the native "Silent" icon could be forced to appear everywhere (status bar and volume panel) by quickly switching the `ringerMode` from `Normal` to `Silent`, skipping the `Vibrate` state entirely. However, doing this forces Android to always activate Do Not Disturb (DND) mode, showing the native DND icon in the status bar. This behavior cannot be bypassed directly.

To prevent the system from forcing DND, this app utilizes an `AutomaticZenRule` (known as a **"Mode"** in Android 15+):

* **Android 15+ (with user-managed modes support):** The app activates a dedicated, custom mode, which successfully bypasses the forced DND activation. Attempting to set the `ringerMode` to silent after the mode is active is ignored by the OS. This is why the volume panel continues to show the vibrate icon.
* **Android 14 and below (or devices lacking user-managed modes support):** Not every device running Android 15+ supports user-managed modes. On these unsupported or older devices, the app falls back to activating the system DND and layering a custom schedule on top. However, you won't be able to customize its specific behavior.

## Why did I develop this app in the first place?
One of the reasons I developed the Quick Settings toggle for sound modes is that Google doesn't provide this option natively on Pixel devices. Currently, the only way to change it is by pressing the volume keys and then adjusting the mode via the small icon that appears. While almost every other Android manufacturer includes a toggle in the Quick Settings, it is curiously missing on Pixels.

Furthermore, I wanted to explore Kotlin as a language and saw this as a great opportunity to get hands-on experience with Android development.

## Find it on Google Play
You can download the App via [GitHub](https://github.com/CurryPlayer/quick-settings-sound-profile/releases) or [Google Play](https://play.google.com/store/apps/details?id=com.curryplayer.quicksettingssoundprofile)

## Credits
For the implementation, I drew inspiration from the already existing [project](https://github.com/Alfio010/sound-quick-settings) by [Alfio010](https://github.com/Alfio010).
