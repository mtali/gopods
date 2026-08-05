# Migration notes

GoPods moved from Views, DataBinding and Fragments to Jetpack Compose, and from
ExoPlayer 2 to Media3. This is what changed and why, in the order it happened.

The app is published, so two things had to survive an in place update: the Room
database and the user's settings. Both are covered under [Data compatibility](#data-compatibility).

## Commits

| Commit | Change |
| --- | --- |
| `build: move to version catalog and upgrade toolchain` | Kotlin DSL, version catalog, AGP 8.13.2, kapt to KSP |
| `refactor: introduce Hilt and restructure the data layer` | Package layout, DI, Retrofit 3 with kotlinx.serialization |
| `feat: move playback to Media3` | MediaSessionService and MediaController |
| `feat: replace the View UI with Compose` | All five screens, Navigation 3, DataStore, Coil |
| `build: move to AGP 9 and current library versions` | AGP 9.2.1, Kotlin 2.4, current AndroidX |
| `feat: offline handling, notification permission and targetSdk 36` | NetworkMonitor, POST_NOTIFICATIONS |
| `chore: R8 rules, release build and docs` | Shrinker rules, release signing, these notes |

Each commit builds and runs. The UI stayed on Views until the Compose commit, so
the toolchain work could be verified separately from the rewrite.

## Structure

One Gradle module, packages arranged as if they were modules:

```
app/         application class, activity, root composable, navigation host
core/        api, common, data, database, datastore, dispatchers, logs,
             models, navigation, player, ui
features/    library, discover, podcast_details, now_playing, settings
sync/        the periodic episode update worker
```

Each feature owns a `navigation/` package holding a `@Serializable` `NavKey`, a
`Navigator` extension and an `EntryProviderScope` function. `app/navigation`
assembles them. Screens are `XxxScreen.kt` containing a stateful `XxxRoute` that
reads a view model and a stateless `XxxScreen` that takes state and callbacks.

## Library changes

| Was | Now |
| --- | --- |
| Views, DataBinding, Fragments, Navigation component, Safe Args | Compose, Navigation 3 |
| appcompat, constraintlayout, swiperefreshlayout, material components | Material 3 |
| androidx.preference | DataStore plus a Compose settings screen |
| Glide | Coil 3, sharing the OkHttp call factory with the api layer |
| ExoPlayer 2.19, MediaBrowserServiceCompat, MediaSessionConnector, support-media-compat | Media3 1.10 |
| Gson and Moshi | kotlinx.serialization |
| Retrofit 2.9, OkHttp 4 | Retrofit 3, OkHttp 5 |
| kapt | KSP |
| material-dialogs, readmore-textview, de.halfbit:edge-to-edge | Material 3 dialogs, an `ExpandableText` composable, `enableEdgeToEdge` |

Hilt was already a dependency but was never wired up: there were no modules, the
application class was not annotated and both view models built their own
repositories and database handle. It is now the actual graph.

## UI

The old app had one screen that switched between the subscription list and live
search results, driven by a `DisplayState` enum and the collapse state of a
`SearchView`. That is now two top level destinations, Library and Discover,
behind a navigation bar that becomes a navigation rail at medium width and above.
A mini player sits above the bar.

Theming is Material 3 with dynamic colour on API 31 and above and a fallback
scheme seeded from the deep purple accent the app shipped with. Light or dark
comes from the theme setting. The 36 accent styles in the old `styles.xml` were
dead code that no screen exposed and are gone, along with
`ThemeUtils.applyChanges`, which was an unimplemented `TODO` on a reachable path.

## Player

`PlaybackService` is a `MediaSessionService` wrapping one `ExoPlayer` and one
`MediaSession`. Media3 owns the notification, lock screen, media buttons, audio
focus and the foreground service lifecycle, so the hand written notification
manager, its Glide bitmap loader, the `MediaSessionConnector` and the
`MediaButtonReceiver` manifest entry all went away.

`PlayerConnection` is a singleton holding the `MediaController` and exposing
`PlayerUiState` as a `StateFlow`, plus a `SharedFlow` of errors. It runs on the
main dispatcher because a controller is main thread only, and ticks position
every 500ms while playing. The activity connects in `onStart` and releases in
`onStop`; the service keeps playing in the background. No UI code touches the
media session.

Episode metadata now travels on the `MediaItem` rather than in a `Bundle` passed
to `playFromUri`, and the `ValueAnimator` that animated the seek bar between
state callbacks is replaced by the position flow.

Two behaviour changes: tapping an episode while another is playing now plays that
episode rather than pausing the current one, and swiping the app away no longer
stops playback.

## Data compatibility

**Room.** The database is still `GoDatabase` at version 1. Entities moved to
`core/database/models` and were renamed, so each one carries an explicit
`@Entity(tableName = ...)` pinning it to the name version 1 shipped with, and
column names are unchanged. Schemas are now exported to `app/schemas`.

This was verified rather than assumed: a build from `master` and a build from the
migration branch produce the same schema identity hash,
`a1ccb5f16ae3bc3db6ac87313f84a516`. Installing the new build over the old one
keeps the subscription list and needs no migration.

**Settings.** DataStore replaces the default SharedPreferences file. The module
registers a `SharedPreferencesMigration` that renames the old keys, so theme,
notification and seek preferences carry over. The last played episode was a
Moshi encoded blob; `NowPlayingEpisode` is now `@Serializable` with the same
field names, so an existing blob still decodes and the mini player restores
after an update.

## Bugs found and fixed on the way

- **Episodes were never persisted.** The old build left the `Episode` table empty
  even after showing a feed, so the hourly update check treated every episode as
  new and would notify about all of them repeatedly. Caused by suspend calls
  inside a blocking `runInTransaction`; now `withTransaction`.
- **Episode notifications could not appear on API 33 and above.** There was no
  `POST_NOTIFICATIONS` declaration. It is now declared and requested when the
  setting is enabled.
- **The notification `PendingIntent` had no mutability flag**, which throws on
  API 31 and above, and reused request code `0` for every podcast so they all
  opened the same feed.
- **WorkManager initialised twice.** With the application class providing the
  configuration, WorkManager's own initializer has to be removed from the
  manifest or the default configuration wins and `HiltWorkerFactory` is never
  used. Caught by `lintVitalRelease`.
- **The RSS parser was pinned to charset ISO-8859-7**, which mangled non ASCII
  text in most feeds.
- **Episode `duration` and `type` were hardcoded empty**; they now come from the
  itunes fields in the feed.
- **`guid` fell back to an empty string**, so feeds without guids collapsed every
  item onto one primary key.
- **iTunes and RSS dates were parsed with the default locale.**
- **A failed refresh with nothing cached showed "No episodes yet"** rather than an
  error with a retry.
- **The episode notification setting summary was inverted**, reading "Hide
  notifications" while notifications were on.
- Requests were issued while offline and sat on a 30 second connect timeout;
  `shouldFetch` now consults connectivity, so the cache or an offline message
  appears immediately and reconnecting retries on its own.

## Decisions worth knowing

- **AGP is pinned to 9.2.1**, not the newer 9.3.1, because Android Studio rejects
  9.3.1 as unsupported.
- **The AGP 9 move waited for the Compose commit.** AGP 9 has built in Kotlin and
  no kapt, and DataBinding needs kapt, so the toolchain could not move until the
  last layout was gone. Keeping them separate also meant the Compose work was
  already committed and working before the toolchain changed underneath it.
- **`usesCleartextTraffic` stays enabled.** Podcast feeds and audio are still
  often served over http, so disabling it would break real subscriptions.
- **No shared element transitions.** Navigation 3 does not expose an
  `AnimatedVisibilityScope` to entry content the way Navigation Compose does, so
  this needs more than a small change. Destination slide transitions and list
  item animations are in place.
- **`minSdk` is 24**, up from 22.

## Not done

Multi module split, a `MediaLibraryService` for Android Auto, downloading
episodes for offline listening (only metadata is cached, audio always streams),
playback speed, a sleep timer, a queue, Baseline Profiles, and tests beyond the
existing smoke tests.
