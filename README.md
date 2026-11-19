# Orange Roulette – Android port (Kotlin)

An **unofficial, non-commercial fan port** of the Flash game *Orange Roulette* to Android,
written in Kotlin. The original game was an Adobe Flash (`.swf`) title whose source code
was never published, and the old Android build can no longer be installed on modern devices.

This repository contains **only the port's source code**. It does **not** contain any
artwork, sounds, music or text from the original game. You need your own copy of
`Orange_Roulette.swf`; a script extracts the resources from it locally.

> This project is not affiliated with, endorsed by or connected to the original author.
> All rights to the game, its artwork, sounds, music and text belong to their respective
> owners (the title screen credits Mikey Houser, with music by Jared H. and Amon26).

## How it works

The game logic was reconstructed by analysing the SWF's ActionScript 3 bytecode
(the game was built with the FlashPunk engine, 700×495 at 40 ticks per second).
The port reproduces the original behaviour as closely as possible:

- a small FlashPunk-style engine (`Engine.kt`): fixed-timestep world, entity update order,
  layered rendering, sprite-sheet animations, blur buffers, sound handling;
- the original entities and global flags ported one-to-one (`Port.kt`);
- rendering on a `SurfaceView` with a hardware canvas (`GameView.kt`).

Currently only the single-player **Classic** mode exists, because that is the only mode
contained in the original SWF.

## Building

Requirements: Android Studio (or Android Code Studio), JDK 17, Python 3 with
`pillow` and `numpy`, and your own `Orange_Roulette.swf`.

```bash
# 1. Extract the resources from your own copy of the game
pip install pillow numpy
python3 tools/extract_assets.py path/to/Orange_Roulette.swf

# 2. Build
./gradlew assembleDebug        # or open the project in Android Studio
```

The repository does not include the Gradle wrapper jar. If `./gradlew` is missing, open the
project in Android Studio (it creates the wrapper) or run
`gradle wrapper --gradle-version 8.11.1`.

Build settings: `compileSdk 36`, `minSdk 21`, Android Gradle Plugin 8.9.1, Kotlin 2.0.21.

Optional: the original uses the free pixel font *04B_03*. If you place it at
`app/src/main/assets/font/04b03.ttf`, the text looks like the original; otherwise a
default font is used.

## Controls

Touch only. Tap to start, tap to advance the story text, and tap the on-screen buttons
(*Spin cylinder*, *Pull trigger*, *Shoot opponent*) during a duel.

## Project layout

```
app/src/main/java/com/canavarport/orangeroulette/
  Engine.kt        FlashPunk-style engine (sprites, world, blur, audio)
  Port.kt          ported game entities and global state
  Game.kt          wrapper that ties the world to the Android view
  GameView.kt      SurfaceView, game loop, touch input
  MainActivity.kt  fullscreen landscape activity
tools/extract_assets.py   extracts gfx/sfx/text from your own SWF
```

## License

The source code in this repository is released under the MIT License (see `LICENSE`).
The license does **not** apply to the original game or any of its assets.

If you are the rights holder and would like this repository changed or removed, please
open an issue and it will be handled promptly.
