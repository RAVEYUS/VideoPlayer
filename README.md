# VideoPlayer

A modern, feature-rich Android video player built with **Media3 ExoPlayer** and **Material Design 3**. This app provides a seamless and immersive viewing experience with advanced playback controls and subtitle management.

## Features

### Playback Excellence
- **ExoPlayer Integration**: High-performance video playback using the latest Media3 library.
- **Continue Watching**: Automatically saves your progress; resume exactly where you left off from the Home screen.
- **Gesture Controls**: Double-tap on the left/right sides to seek backward or forward.
- **Playback Speed**: Adjust video speed from 0.5x up to 2.0x to suit your preference.
- **Aspect Ratio**: Toggle between Fit, Fill, and Zoom modes for the perfect view.

### Subtitle Management
- **Online Search**: Search and download subtitles directly within the app (powered by OpenSubtitles).
- **Local Subtitles**: Load `.srt` or other subtitle files directly from your device storage.
- **Customization**: Adjust subtitle font size and sync timing with subtitle delay settings.

### Organization & UI
- **Folder Browsing**: Automatically scans and organizes your videos into folders.
- **Favorites**: Mark your favorite videos for quick access on the dashboard.
- **Material 3 UI**: Modern interface with dynamic colors and smooth transitions (FadeThrough and ContainerTransform).
- **Screen Lock**: Lock the player controls to prevent accidental touches while watching.
- **Wavy Progress Bar**: A stylish, animated seeker that adds a modern touch to the player.

## Tech Stack
- **Language**: Java
- **UI Framework**: XML with Material Components
- **Video Engine**: [AndroidX Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3)
- **Image Loading**: [Glide](https://github.com/bumptech/glide)
- **Networking**: OkHttp & Gson
- **Animations**: Material Motion

## Screenshots
*(Add screenshots here)*

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/videoplayer.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and build the project.
4. Run on a physical device or emulator (API 24+ recommended).

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

### How to Contribute
1. **Fork** the Project.
2. Create your **Feature Branch** (`git checkout -b feature/AmazingFeature`).
3. **Commit** your Changes (`git commit -m 'Add some AmazingFeature'`).
4. **Push** to the Branch (`git push origin feature/AmazingFeature`).
5. Open a **Pull Request**.

### Guidelines
- Follow the existing code style (Java).
- Ensure your changes don't break existing functionality.
- Update the README if you add new features.

## License
Distributed under the MIT License. See `LICENSE` for more information.

---
