# Screenshots

These are the images the project README links to, and the same set is used for the Play
Store listing. They are tracked because the README renders them on GitHub, so they cannot
live in an ignored directory.

Captured at 1080x2400 on a Medium Phone API 36 emulator in the dark theme, then written
out as JPEG at quality 82. Lossless PNG captures go in `/screenshots/raw`, which is git
ignored.

| File | Screen |
| --- | --- |
| `01-library.jpg` | Subscription grid with the mini player |
| `02-discover.jpg` | Search results |
| `03-details.jpg` | Podcast header and episode list, with the playing episode marked |
| `04-now-playing.jpg` | Player with episode notes and speed |
| `05-settings.jpg` | Theme, dynamic colour, notifications, seek step |
| `06-lockscreen.jpg` | Lock screen transport controls during background playback |

## Recapturing

Install a debug build on a device or emulator, subscribe to a few podcasts so the grid has
artwork, start an episode so the mini player and the notification appear, then:

```
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png screenshots/raw/01-library.png
sips -s format jpeg -s formatOptions 82 screenshots/raw/01-library.png \
  --out docs/screenshots/01-library.jpg
```

The lock screen shot needs a screen lock set, otherwise the emulator wakes straight back
into the app:

```
adb shell locksettings set-pin 1234
adb shell input keyevent 26 && sleep 3 && adb shell input keyevent 26
# capture, then
adb shell locksettings clear --old 1234
```

## Play Store note

These are the device's native 1080x2400, which is what a modern phone produces. If the
Play Console rejects the aspect ratio, crop to 9:16 rather than scaling, so the pixels
stay sharp:

```
sips -c 1920 1080 docs/screenshots/01-library.jpg --out play-01-library.jpg
```
