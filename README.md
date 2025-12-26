# SLO at a Glance - Project Summary

## Overview
An Android mobile application for monitoring Instana Service Level Objectives (SLOs) with intuitive traffic light visualization (Green/Yellow/Red status indicators).

## Key Features
- 🚦 **Traffic Light Visualization** - Visual status indicators for each SLO
- 📊 **Detailed SLO Reports** - View SLI, SLO targets, error budgets, and trend charts
- 🔍 **Dual Filtering System** - Filter by status and entity type
- ✅ **API Validation** - One-click validation of API connection and credentials
- ⚙️ **Configurable Thresholds** - Customize yellow threshold percentage
- 🔒 **Secure Storage** - Encrypted storage for API credentials
- 🔄 **Pull-to-Refresh** - Easy data refresh with swipe gesture

## Technology Stack
- **Language**: Java
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

### Key Libraries
- Retrofit 2.9.0 - REST API client
- OkHttp 4.12.0 - HTTP client with logging
- Gson 2.10.1 - JSON serialization
- MPAndroidChart 3.1.0 - Line charts for visualization
- AndroidX Security 1.1.0 - Encrypted SharedPreferences
- Material Design 3 - Modern UI components

## Project Structure
```
slo-at-a-glance/
├── slo-mobile/                    # Main application module
│   ├── src/main/
│   │   ├── java/io/instana/slo/  # Package: io.instana.slo
│   │   │   ├── MainActivity.java
│   │   │   ├── data/
│   │   │   │   ├── model/        # Data models (Slo, SloReport, etc.)
│   │   │   │   ├── api/          # Retrofit API service
│   │   │   │   └── repository/   # Repository pattern
│   │   │   ├── ui/
│   │   │   │   ├── slolist/      # SLO list screen
│   │   │   │   ├── slodetail/    # SLO detail screen
│   │   │   │   └── settings/     # Settings screen
│   │   │   └── util/             # Utility classes
│   │   └── res/                  # Resources (layouts, drawables, etc.)
│   └── build.gradle
└── README.md
```

## API Integration
The app connects to Instana API endpoints:
- **SLO List**: `GET /api/settings/slo`
- **SLO Report**: `GET /api/slo/report?sloId={id}`
- **Version (Validation)**: `GET /api/instana/version`

Authentication: `Authorization: apiToken {your-token}`

## Traffic Light Logic
1. 🔴 **RED**: SLI ≤ SLO Target (SLO not being met)
2. 🟡 **YELLOW**: Error budget remaining ≤ Yellow threshold
3. 🟢 **GREEN**: SLO is healthy

## Recent Enhancements

### API Validation Feature
- Added "Validate" button in Settings > API Configuration
- Tests connection to `/api/instana/version` endpoint
- Displays version information on success
- Shows detailed error messages on failure

### Chart Improvements
- Hidden individual data point values for cleaner visualization
- Changed x-axis to display dates only (MM/dd format)
- Rotated x-axis labels vertically for better readability

### Package Refactoring
- Migrated from `com.instana.slo` to `io.instana.slo`
- Updated all 18 Java source files
- Updated build configuration and namespace

## Build Instructions

### Debug Build
```bash
# Windows
gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

### Clean Build
```bash
gradlew.bat clean
```

## Configuration
Users configure the app through Settings:
1. **API Endpoint** - Instana API base URL
2. **API Token** - Authentication token
3. **Yellow Threshold** - Error budget percentage for yellow status (default: 50%)
4. **SLO Selection** - Choose which SLOs to display

## Security
- API tokens stored using EncryptedSharedPreferences
- All API communication uses HTTPS
- ProGuard obfuscation enabled in release builds

## Version
**Current Version**: 1.0

## License
Developed for Instana API integration.