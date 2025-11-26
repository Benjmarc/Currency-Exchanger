# Currency Exchanger

An Android application for currency conversion with real-time exchange rates.

## 🚀 Features

- Real-time currency conversion
- Support for multiple currencies
- Clean architecture with MVVM pattern
- Offline support
- Unit tests

## 🛠 Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Asynchronous**: Kotlin Coroutines, Flow
- **Networking**: Retrofit
- **Database**: Room
- **Testing**: JUnit, MockK, Espresso
- **Build System**: Gradle

## 📋 Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Java 17 or later
- Gradle 8.0 or later

## 🚀 Getting Started

### 1. Open the project in Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned repository and click "OK"
4. Let Android Studio sync the project and download dependencies

### 2. Build and Run

1. Connect an Android device or start an emulator
2. Click on the "Run" button in Android Studio (or press Shift + F10)
3. Select your target device and click "OK"

### 3. Running Tests

#### Unit Tests

```bash
./gradlew test
```

#### Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

## 🏗 Project Structure

```
app/
├── src/
│   ├── main/           # Main source code
│   │   ├── java/com/currency/exchanger/
│   │   │   ├── data/           # Data layer
│   │   │   ├── di/             # Dependency Injection
│   │   │   ├── domain/         # Business logic
│   │   │   └── presentation/   # UI layer
│   │   └── res/                # Resources
│   └── test/           # Unit tests
│   └── androidTest/    # Instrumentation tests
├── build.gradle        # App level build configuration
└── ...
```

## 🔧 Configuration

### Environment Variables

Create a `local.properties` file in the root directory and add your API key:

```properties
API_KEY=your_api_key_here
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 Contact

Benj Marc - [@your_twitter](https://twitter.com/your_twitter)

Project Link: [https://github.com/Benjmarc/Currency-Exchanger](https://github.com/Benjmarc/Currency-Exchanger)
