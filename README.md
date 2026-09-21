# Simple Counter
This is a simple counter with customization and passcode locking features, made specifically for EspeRiciclo at ITIS Paleocapa, but here available for general purpose and usage.

## Features
- Simple counter with buttons for increasing and decreasing value and manual input options
- Fully customizable multiplier <!-- Other operation types and multiple numbers to release with 1.1 -->
- Shows date & time on top left <!-- Any position on 1.1 -->
- Passcode protected with three finger tap gesture (default code is 1234) <!-- Becomes 0000 in 1.1 -->
- Fully customizable with color and font options
<!-- - History graph with value changes over time planned for 1.1 -->

## Getting Started
1. Download the latest release from the [Release](https://github.com/mattiagalli-sudo/simple-counter/releases) page.
2. Install the `apk.` file on the Android device. If requested, allow installation from unknown sources.
If you're unable to install the app because your device thinks it's "not safe" that means Google locked your Android phone. You have to use the advanced process to install it. More info: https://keepandroidopen.org/

## Compatibility
Here are devices on which i tested the app and it works:
- Samsung Android 5 tablet

Here are devices on which i tested the app and it doesn't work:
- Xiaomi Android 15 phone

## Build
In this repository, the app is built using Gradle in GitHub Actions, so the files you need to install the app are in the Releases page. If you still want to build the app yourself, you must download the source code from the repository (for latest available code, which may be buggy depending on the development state) or the Releases page (for a specific release's source code), and run it through the compiler of your choice (Gradle is suggested). I suggest building the latest available stable release.

## Docs
Documentation has been migrated in the [wiki](https://github.com/mattiagalli-sudo/simple-counter/wiki/).

## Roadmap
### 1.1
- Make bigger UI sizes available
- Allow editing clock size and add more formats.
- Allow changing the clock position.
- Allow text to be added before the counter (in 1.0 it can only be added after the counter).
- Remove char limits for text boxes.
- Add a counter history (logs once every hour at XX:00 if an edit has been made) with charts showing progress.
- Allow multiple multipliers per counter, to show more information.
- Allow more kinds of operations for the additional number/multiplier.
- Allow showing and hiding the clock with a toggle in the settings.
- Set default code as 0000, as it's a more common default passcode.
- Add full screen view.
- Allow screen to stay on while the app is in foreground.

### 1.2
- Add multiple counters, appearing as a menu at the bottom left (they're visible in the locked state, but only editable in the unlocked state). Each counter has it's own multipliers.
- Create a directory for various language files in the source code (this simplifies localization in other languages).
- Add Italian localization.
- Add a language selector in settings.
- Allow for editing the frequency with which the history gets updated.
- Add an app icon.
- Allow to image as background.
- Avoid cursor to autofocus on text box when opening settings.
- Allow importing and exporting of all data (settings, counter values, history...) in open file formats (such as .yaml).
- Allow for customizing the + and - buttons to increase and decrease input of a certain value.

### Maybe, in the future...
- Data syncing (not useful: could just work with Syncthing set up in the files directory. It could be nice to store all app data in a folder to make this easier).
- Permit to resize UI elements separately.
- Integrate system voice for reading counters when touching the screen, to assist blind people and make it easier for them to access the data. (Not easy to implement, requires the device having a TTS model, which is rare expecially on old phones and tablets. Should ship the app with a built-in or optionally downloadable TTS model for universal compatibility.
- Add more types of graphs (such as histograms).
- Web UI and API to edit the counter from multiple devices.

## AI usage
Most of the code is written by AI due to lack of the competences. All the creative direction, as well as the docs, readme, and localization (excluding English) writing is done by the author of Simple Counter (me) and the project contributors (thank you!).

## Screenshots
### 1.0
<img width="640" height="400" alt="Locked state" src="https://github.com/user-attachments/assets/83f33327-7f10-41d9-98fd-415721a92646" />
<img width="640" height="400" alt="Unlocked state" src="https://github.com/user-attachments/assets/1a8fa572-a946-40e9-8a23-5cac98362732" />
<img width="640" height="400" alt="Settings (1)" src="https://github.com/user-attachments/assets/bc4a53c7-8dc1-4989-95e2-f3daf03fb826" />
<img width="640" height="400" alt="Settings (2)" src="https://github.com/user-attachments/assets/47a2d8f6-d8fd-4158-bd04-51a9f5741904" />
<img width="640" height="400" alt="Usage example" src="https://github.com/user-attachments/assets/29d94d5f-cbb3-4aea-84c0-664cd5616523" />
