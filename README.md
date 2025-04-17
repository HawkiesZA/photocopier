# Photocopier

A macOS desktop app (Kotlin Multiplatform + Compose Multiplatform) to copy photos from an SD card to an external hard drive, organizing them by date taken (YYYY/MM-DD).

## Features
- Native macOS look (Compose Multiplatform)
- Select SD card and target directory
- Copies photos into YYYY/MM-DD folders based on EXIF date
- Progress bar and status display

## Getting Started
1. Install [Kotlin](https://kotlinlang.org/docs/command-line.html) and [JDK 17+](https://adoptium.net/)
2. Run: `./gradlew run`

## TODO
- Extract real date from EXIF metadata (currently stubbed)
- Improve error handling
- Package as DMG for easy install
