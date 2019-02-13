
# Debugging android app from command line using `adb` and `jdb`

## Set up

Issue `adb jdwp` in order to find out PID of the debuggable java
process running on the connected android device.

Set up forwarding to a local port on the developer's host machine
using `adb forward tcp:7777 jdwp:<PID>`, where `<PID>` is the
(last) PID returned by the previous command.

## Clean up

Eventually, once finished, issue `adb forward --remove-all`
to clear up the forwarding.

## Debugging

This is how the debugger is started:

`jdb -sourcepath src/ -attach localhost:7777`

### Debugger commands

* `stop in <method_name>`
* `stop at <class>:<line_number>`
* `cont`
* `print <variable_name>`
* `dump <variable_name>`

# Existing UI description

## Overview of UI features of the Video Player

* single tap
  * shows/hides the controls and file name
* double tap, depending on position
  * seek back/rewind 10 seconds back
  * seek forward 10 seconds forward
  * pause/resume
* drag gesture depending on the starting point and direction:
  * adjust brightness
  * adjust volume
  * seek forward,backwards

## Overview of UI features of the Audio Player:

In the audio player, there are multiple (two?) fragments.
I assume one fragment is for the playlist and the other fragment
is a "player" which shows two things: (1) graphic representing
the current song and (2) the playback controls.

There is always a strip at the top of the player which shows
the name of the current song. The strip also contains a button
which only seems to work in the portrait mode, although it is shown
in the landscape mode also. Moreover in portrait mode the
button only works when the playback is stopped. This button
makes it possible to replace the graphic (1) with the playlist
(which is in the landscape mode shown in the right half of the screen
together with the "player" part)

# The Video Player Activity `VideoPlayerActivity`

# Important data members:

`mService` of type `org.videolan.vlc.PlaybackService`

## Interfaces it implements that are used by `VideoTouchDelegate`:

* `android.content.Context` is accepted by the
  `android.view.ScaleGestureDetector` constructor

## Methods and data members used by `VideoTouchDelegate`:

### Purpose unclear

* make clear what is the purpose of these methods:
  * `isPlaybackSettingActive`
  * `endPlaybackSetting`
  * `isPlaylistVisible`
  * `togglePlaylist`
  * `isLocked`
  * `toggleOverlay`
  * `initAudioVolume` // Used on `ACTION_DOWN`
  * `sendMouseEvent`
  * `updateViewpoint`
  * `isOnPrimaryDisplay`
  * `navigateDvdMenu`
  * `changeBrightness`
  * `hideOverlay`
  * `showInfo`
  * `displayWarningToast`
  * `setVideoScale`
* data members with purpose unclear:
  * `isPlaybackSettingActive`
  * `fov`
  * `isLoading`
  * `audiomanager`
  * `mService`
  * `isAudioBoostEnabled`
  * `currentScaleType`
  * `handler`

### Pausing/restoring playback:
      
* methods
  * `doPlayPause`

### Brightness setting:

* methods:
  * `changeBrightness`
* data members:
  * `window`

### Seeking:

* methods:
  * `seek`
  * `seekDelta`

### Volumen setting:

* methods:
  * `setAudioVolume`
* data members:
  * `audioMax`
  * `volume`
  * `originalVol`

# Find What results to be `VideoTouchDelegate`

It's corresponding layout is probably `player_hud`.

It is being created and used by the `VideoPlayerActivity`
in the `org.videolan.vlc.gui.video` package.

It is being instantiated near the end of the `onCreate` method.

# Inspect `VideoTouchDelegate` and `VideoPlayerActivity`'s Inteface

Find Whether this can be used in Audio.

In particular:

* See the `VideoTouchDelegate.doSeekTouch` method.
* Refactor `VideoTouchDelegate`'s dependencies:
  * Dependency on:
* `VideoTouchDelegate` constructor's parameters:
  * `player` - `VideoPlayerActivity`
  * `mTouchControls` - bit field defining what shall be controlled
  * `screenConfig`
     * `DisplayMetrics`,
     * `xRange`, `yRange`,
     * orientation
  * `tv` - boolean
* See the interface of `VideoPlayerActivity` that
  the `VideoTouchDelegate` actually uses.
* See `VideoTouchDelegate`'s dependencies.
* Try to move refactored `VideoTouchDelegate` outside
  of its original package.
 
# Find the widgets used for Audio Playback.

Regarding the UI definition, there is:
* `audioplayer.xml` - a Fragment and
* `audio_player.xml` - 

There is `AudioPlayer.kt`
See its `onTouch` method.
See `PlaylistModel.time` data member.

I also noticed there is `AudioMediaSwitcher` java class
which has something to do with playlist item switching.

# Other stuff

* see `PlaylistModel.switchToVideo` and the `service`
  (`org.videolan.vlc.PlaybackService`) data member

# Zkušenosti se sluchátky

- mít možnost ovládat přehrávání a přijímat hovory bez
  manipulace s telefonem je skvělé

