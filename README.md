# LingoLens — AI Visual Translation & Travel Companion

<p align="center">
  <b>An editorial-grade, offline-capable AI translation and visual OCR lens Android application inspired by Mastercard & Lovable design systems.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/Framework-Jetpack%20Compose-blue?style=flat-square" alt="Compose">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square" alt="Kotlin">
  <img src="https://img.shields.io/badge/Design-Mastercard%20%2F%20Lovable-orange?style=flat-square" alt="Design System">
</p>

---

## 🌟 Overview

**LingoLens** is a next-generation Android translation companion built with Kotlin and Jetpack Compose. It combines on-device Optical Character Recognition (OCR), real-time camera viewfinder translation overlays, instant speech translation, offline language packs, and currency conversion into a warm, putty-cream editorial interface styled after Mastercard and Lovable design principles.

---

## ✨ Key Features

1. **Visual OCR Lens & Camera Translation**
   - Live Camera viewfinder with real-time text recognition and bounding box translation overlays.
   - Tap-to-focus, pinch-to-zoom, torch control, and camera lens switching (front/rear).
   - Freeze-frame inspection mode for reading distant street signs, menus, and documents.
   - Photo Gallery OCR picker to translate existing screenshots and images instantly.

2. **Speech & Conversation Translation**
   - Real-time voice translation with microphone recording and Text-to-Speech (TTS) audio playback.
   - Dual-pane conversation mode for seamless face-to-face cross-lingual dialogues.

3. **Travel Phrasebook & Cultural Guide**
   - Essential phrase categories (Dining, Emergency, Transport, Shopping, Hotel).
   - Audio pronunciation aids and quick copy/favorite tools.

4. **Offline Translation & Currency Converter**
   - Downloadable ML Kit language translation models for offline functionality.
   - Real-time exchange rate currency calculator for global travelers.

5. **Mastercard & Lovable Editorial Design System**
   - **Canvas Cream Surface (`#F3F0EE` / `#F7F4ED`)**: Warm, putty-toned background replacing cold sterile white.
   - **Ink Black CTAs (`#141413` / `#1C1C1C`)**: 20px radius primary action pills with high contrast.
   - **Signal Orange Accents (`#CF4500`)**: Precision highlights reserved for consent and critical status alerts.
   - **Circular Portraits & Satellite CTAs**: Distinctive constellation layout with orbital decorative arcs.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: Jetpack Compose & Material 3 with edge-to-edge support.
- **Architecture**: Clean Architecture / MVVM with Kotlin Coroutines and StateFlow.
- **Camera & Vision**: CameraX, Google ML Kit Text Recognition & Translation.
- **State & Navigation**: Type-safe navigation and reactive state management.
- **Storage**: Room database & DataStore preferences.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK (compileSdk 34, minSdk 26)

### Build Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/piyushkumar920/lingolens-android.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the app on an emulator or physical device.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
