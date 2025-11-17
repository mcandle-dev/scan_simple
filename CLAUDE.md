# CLAUDE.md - Project Context for AI Assistance

## Project Overview
**VPOS BLE App Test** is a minimal Android application designed for experimenting with Bluetooth Low Energy (BLE) scanning using a vendor-provided library (`libVpos3893_release_20250930.aar`). The app demonstrates how to scan for BLE devices, parse advertisement data, and display results in JSON format.

## Tech Stack
- **Language**: Kotlin (MainActivity) + Java (BleScan)
- **Platform**: Android
- **Compile SDK**: 35
- **Target SDK**: 35
- **Minimum SDK**: 24
- **Java Version**: 11
- **Build System**: Gradle with Kotlin DSL (build.gradle.kts)
- **External Library**: `libVpos3893_release_20250930.aar` (VPOS BLE library)
- **Concurrency**: Kotlin Coroutines + Java Threads
- **NDK Support**: armeabi-v7a, arm64-v8a, x86, x86_64

## Project Structure

```
scan_simple/
├── app/
│   ├── libs/
│   │   └── libVpos3893_release_20250930.aar  # External VPOS BLE library (release version)
│   ├── log/
│   │   ├── simple.txt                         # Sample log output
│   │   └── orginal.txt                        # Original log output
│   ├── src/
│   │   ├── androidTest/
│   │   │   └── java/com/example/test1/
│   │   │       └── ExampleInstrumentedTest.kt # Android instrumentation tests
│   │   ├── main/
│   │   │   ├── java/com/example/test1/
│   │   │   │   ├── MainActivity.kt            # Main activity with UI controls
│   │   │   │   └── BleScan.java              # BLE scanning logic wrapper
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml     # UI layout with 3 buttons
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── values-night/
│   │   │   │   │   └── themes.xml            # Dark theme support
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       └── data_extraction_rules.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/example/test1/
│   │           └── ExampleUnitTest.kt         # Unit tests
│   ├── build.gradle.kts                       # App-level build configuration (Kotlin DSL)
│   └── proguard-rules.pro                     # ProGuard configuration
├── gradle/
│   ├── libs.versions.toml                     # Version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                           # Project-level build configuration
├── settings.gradle.kts                        # Project settings
├── gradle.properties                          # Gradle properties
├── gradlew                                    # Gradle wrapper (Unix)
├── gradlew.bat                                # Gradle wrapper (Windows)
├── README.md                                  # User-facing documentation
├── TODO.md                                    # Project roadmap and tasks
└── CLAUDE.md (this file)                      # AI assistant context
```

## Key Components

### 1. MainActivity.kt (`app/src/main/java/com/example/test1/MainActivity.kt`)
Main activity that provides the UI and coordinates BLE scanning operations.

**Key Features**:
- Three buttons: "Master", "Scan", "ComRev"
- Manages scanning state with coroutines
- Receives scan results via callbacks

**Button Functions**:
- **btn1 (Master)**: Calls `Step1()` → Enables BLE master mode and gets device MAC address
- **btn2 (Scan)**: Calls `Step2()` → Starts a one-time BLE scan
- **btn3 (ComRev)**: Calls `toggleScan()` → Toggles continuous scanning on/off

**Important Methods**:
- `Step1()`: Enables master mode using `bleScan.enableMasterMode(true)` and retrieves device MAC
- `Step2()`: Starts new scan with `bleScan.startNewScan()` (one-time scan)
- `Step3()`: Starts async scan with result listener, runs in `Dispatchers.IO` coroutine
- `toggleScan()`: Manages continuous scanning state, disables other buttons while scanning
- `startScan()` (lines 123-134): **UNUSED** - Duplicate code, should be removed per TODO.md

**Key State Variables**:
- `isScanning`: Boolean flag to track scanning state
- `scanJob`: Job reference for cancelling coroutine
- `bleScan`: Instance of BleScan class

**Callback Implementations**:
- `DataReceiveListener` callback (line 58-63): Receives raw buffer data from `Lib_ComRecvAT()`
- `ScanResultListener` callback (line 83-90): Receives parsed JSON results from `recvScanData()`

**UI State Management**:
- When scanning starts: btn3 text → "Stop", btn1/btn2 disabled
- When scanning stops: btn3 text → "ComRev", btn1/btn2 enabled

### 2. BleScan.java (`app/src/main/java/com/example/test1/BleScan.java`)
Wrapper class for the VPOS BLE library's `At` API.

**Key Features**:
- Wraps native `At` library calls
- Parses BLE advertisement data (ADV/RSP packets)
- Provides callback interfaces for data reception
- Runs scanning in background threads

**Core Methods**:
- `enableMasterMode(boolean)`: Enables/disables BLE master mode, checks `isMaster` flag to prevent redundant calls
- `getDeviceMacAddress()`: Returns device MAC address via `At.Lib_GetAtMac()`
- `startNewScan(String, String, int, String, String)`: Initiates a new BLE scan with filters (MAC, name, RSSI, mfg ID, data)
- `startScan(SharedPreferences, ScanResultListener)`: **DUPLICATE** - Similar to `startScanAsync`, creates new Thread
- `startScanAsync(SharedPreferences, ScanResultListener)`: Starts async scanning, calls `startScan()` then `recvScanData()`
- `stopScan()`: Sets `isScanning = false` and calls `At.Lib_AtStopScan()`
- `ComRecvAT()`: **TEST METHOD** - Receives raw data without parsing, used for debugging
- `recvScanData(ScanResultListener)`: Main scanning loop - receives, parses, and returns JSON results
- `parseAdvertisementData(byte[])`: Static method to parse BLE GAP data types into JSON
- `hexStringToByteArray(String)`: Converts hex string to byte array
- `bytesToHex(byte[])`: Converts byte array to hex string

**Key State Variables**:
- `isScanning`: Boolean flag controlling the scan loop
- `isMaster`: Boolean flag tracking master mode state

**Callback Interfaces**:
```java
public interface ScanResultListener {
    void onScanResult(JSONArray scanData);
}

public interface DataReceiveListener {
    void onDataReceived(String buff);
}
```

**Data Structures**:
- `deviceMap`: `ConcurrentHashMap<String, JSONObject>` - Maps MAC addresses to device data
- `lineLeft`: String buffer for incomplete data lines from previous iteration

**Data Flow**:
```
At.Lib_ComRecvAT()
  → recvScanData()
    → Parse MAC/RSSI/Payload
      → parseAdvertisementData()
        → JSON result
          → ScanResultListener callback
            → MainActivity UI update
```

### 3. VPOS Library (`libVpos3893_release_20250930.aar`)
External vendor library providing the `At` API for BLE operations. This is a **release version** (previous version was debug build from March 2025, now updated to September 2025 release build).

**Hardware Architecture**:
The library communicates with an **EFR32BG22 BLE module** via JNI and serial/UART communication.

**Key API Methods**:
- `At.Lib_EnableMaster(boolean)`: Enable/disable master mode
- `At.Lib_EnableBeacon(boolean)`: Enable/disable beacon mode
- `At.Lib_GetAtMac(String[])`: Get device MAC address
- `At.Lib_AtStartNewScan(String, String, int, String, String)`: Start BLE scan with filters (MAC, name, RSSI, mfg ID, data)
- `At.Lib_ComRecvAT(byte[], int[], int, int)`: Receive scan data from BLE module
- `At.Lib_AtStopScan()`: Stop scanning
- `At.Lib_GetBeaconParams(Beacon)`: Query beacon parameters
- `At.Lib_SetBeaconParams(Beacon)`: Update beacon parameters

**Library Loading**:
The library is included via Gradle Kotlin DSL:
```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```

### 4. Log Samples (`app/log/`)
The project includes sample log outputs for reference:

- **`simple.txt`**: Sample scan results and data output
- **`orginal.txt`**: Original/reference log output

These files provide examples of:
- Raw data format from `Lib_ComRecvAT()`
- Expected MAC address formats
- RSSI values and advertisement payload examples
- Typical BLE scanning output patterns

Use these logs to understand expected data formats when debugging parsing issues.

## BLE Scanning Flow

### Initialization
1. User presses "Master" button
2. `enableMasterMode(true)` is called
3. Device MAC address is retrieved

### Single Scan
1. User presses "Scan" button
2. `startNewScan()` initiates scan with `Lib_AtStartNewScan`
3. Scan runs once

### Continuous Scan
1. User presses "ComRev" button
2. `toggleScan()` starts continuous scanning
3. `Step3()` runs in coroutine on IO dispatcher
4. `recvScanData()` loops while `isScanning == true`:
   - Calls `Lib_ComRecvAT()` to receive raw data
   - Parses lines starting with "MAC:"
   - Extracts MAC, RSSI, ADV/RSP payloads
   - Converts hex payload to bytes
   - Parses advertisement data (flags, UUIDs, device name, etc.)
   - Builds JSON object per device
   - Calls `ScanResultListener.onScanResult()` with JSONArray
5. MainActivity receives results and logs them

### Data Format
Raw data format from `Lib_ComRecvAT`:
```
MAC:<address>,RSSI:<value>,ADV:<hex_payload>
MAC:<address>,RSSI:<value>,RSP:<hex_payload>
```

Parsed JSON format:
```json
[
  {
    "MAC": "AA:BB:CC:DD:EE:FF",
    "RSSI": -50,
    "ADV_org": "02010611FF...",
    "ADV": {
      "Flags": "06",
      "Device Name": "MyDevice",
      "Manufacturer Data": "..."
    },
    "Timestamp": 1234567890
  }
]
```

## Build & Run

### Build
```bash
./gradlew assembleDebug
```

### Install
```bash
./gradlew installDebug
```

### Run
Open the app and:
1. Grant Bluetooth permissions if prompted
2. Press "Master" to enable master mode
3. Press "Scan" for single scan OR "ComRev" for continuous scanning
4. Check Logcat for scan results

## Important Notes

### State Management
- `isScanning` flag in `BleScan` controls the scan loop
- `isMaster` flag prevents redundant master mode calls
- Coroutine `scanJob` is used for cancellable background scanning

### Threading
- MainActivity runs on main/UI thread
- `Step3()` launches on `Dispatchers.IO`
- `recvScanData()` runs in background thread (via `Thread()` or coroutine)
- UI updates use `runOnUiThread {}`

### Callback Pattern
Two callback interfaces are defined:
1. `DataReceiveListener`: Raw data callback (used in `ComRecvAT()`)
2. `ScanResultListener`: Parsed JSON results callback (used in `recvScanData()`)

### Parsing Logic

**Data Reception** (`recvScanData()` in BleScan.java:141-265):
1. Data is received as byte array from `Lib_ComRecvAT(recvData, recvDataLen, 20, 1000)`
   - `recvData`: 2048-byte buffer
   - `recvDataLen`: Array storing actual received length
   - Timeout: 20ms, Max wait: 1000ms

2. Lines are split by `\r\n|\r|\n` using regex pattern
3. Last incomplete line is carried over to next iteration via `lineLeft` buffer
4. Only lines starting with "MAC:" are processed

**Line Format**:
```
MAC:<address>,RSSI:<value>,ADV:<hex_payload>
MAC:<address>,RSSI:<value>,RSP:<hex_payload>
```

**Advertisement Data Parsing** (`parseAdvertisementData()` in BleScan.java:291-368):
- Parses according to Bluetooth Core Spec GAP data types
- Format: `[Length][Type][Data]` repeated
- Supported types:
  - `0x01`: Flags
  - `0x02/0x03`: 16-bit Service UUIDs (incomplete/complete)
  - `0x04/0x05`: 32-bit Service UUIDs (incomplete/complete)
  - `0x06/0x07`: 128-bit Service UUIDs (incomplete/complete)
  - `0x08/0x09`: Device Name (shortened/complete)
  - `0x0A`: TX Power Level
  - `0x16`: Service Data - 16-bit UUID
  - `0x20`: Service Data - 32-bit UUID
  - `0x21`: Service Data - 128-bit UUID
  - `0xFF`: Manufacturer Specific Data

**Data Structure Evolution**:
1. Raw bytes → Hex string (from `Lib_ComRecvAT`)
2. Hex string → Byte array (`hexStringToByteArray`)
3. Byte array → JSON object (`parseAdvertisementData`)
4. JSON objects → JSONArray (`deviceMap` snapshot)
5. JSONArray → Callback listener (`onScanResult`)

**Critical Implementation Details**:
- `deviceMap` is created **inside the while loop** at line 151, causing potential data loss
- `deviceMap` snapshot is created **inside the for loop** at line 257, sending results prematurely
- RSSI value is negated in `startNewScan`: `-rssi` (line 66)
- Payload length validation: Must be ≤62 chars and even length (line 188-189)
- Chinese character encoding issues in comments (lines 157, 164, 182, 241, 252)

### Known Issues
- Some Chinese comments in code (encoding artifacts like `����` visible)
- `startScan()` method duplicates logic with `startScanAsync()`
- Error handling could be improved (many `continue` statements in loops)
- No proper resource cleanup in MainActivity `onDestroy()`

## Permissions Required

**IMPORTANT**: The current `AndroidManifest.xml` does **NOT** have any BLE permissions declared. For proper BLE functionality on Android devices, the following permissions should be added:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="s" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

**Permissions breakdown**:
- `BLUETOOTH` - Basic Bluetooth operations (legacy, < Android 12)
- `BLUETOOTH_ADMIN` - Bluetooth discovery and settings (legacy, < Android 12)
- `BLUETOOTH_SCAN` - BLE scanning (Android 12+)
- `BLUETOOTH_CONNECT` - Connect to BLE devices (Android 12+)
- `ACCESS_FINE_LOCATION` - Required for BLE scanning on Android (location services)

**Runtime Permissions**:
For Android 6.0+ (API 23+), location permissions must be requested at runtime. The current implementation does **not** handle runtime permission requests, which may cause BLE scanning to fail on real devices.

## Debug Tips
- Check Logcat with filter: `TAG` or `BLE_SCAN` or `MainActivity`
- Common log messages:
  - `"Lib_ComRecvAT buff:"` - Raw received data
  - `"Received Scan Data:"` - Parsed JSON results
  - `"Master mode updated successfully"` - Master mode status
  - `"BLE Scan Started with result:"` - Scan initialization

## Development Context

**Current Version**: 1.0
**Last Updated**: 2025-11-17
**Repository**: `scan_simple/`

**Recent Commits** (most recent first):
1. `46a8cb1` - Update README with AT Class API and BLE scanning flow
2. `5e6a91c` - Add project documentation and update library
3. `843bfa0` - Merge pull request #1 (README updates)
4. `ae2d62d` - Update README with build and BLE info
5. `9d5de35` - Create README.md
6. `35db6f4` - 1.0 버전 (1.0 release in Korean)

**Key Changes from Initial Release**:
- Library updated from debug build (`libVpos3893_debug_20250307.aar`) to release build (`libVpos3893_release_20250930.aar`)
- Added comprehensive documentation (README.md, CLAUDE.md, TODO.md)
- Updated README with detailed AT Class API reference and architecture diagrams
- Added log samples in `app/log/` directory

**Git Branch**: Currently on `claude/claude-md-mi2hru40nloe11my-01FerWFNejU8HDDq6wwpU7zf`

## Working with This Project

### Development Guidelines for AI Assistants

When modifying this project, keep in mind:

1. **Proprietary Library Constraints**:
   - The VPOS library (`libVpos3893_release_20250930.aar`) is proprietary - its behavior is a black box
   - The library communicates with EFR32BG22 hardware via UART/serial
   - API documentation is limited to observed behavior and README examples
   - Always test changes with the actual hardware/library combination

2. **Permission Management**:
   - BLE scanning requires proper permission handling on Android
   - **Current Issue**: AndroidManifest.xml is missing required BLE permissions
   - Runtime permission requests are NOT implemented (needs to be added)
   - Location permissions are mandatory for BLE scanning on Android

3. **Threading Architecture**:
   - Threading is mixed (Kotlin coroutines + Java threads)
   - MainActivity uses `lifecycleScope` and `CoroutineScope(Dispatchers.IO)`
   - BleScan.java uses `Thread()` and blocking operations
   - UI updates must use `runOnUiThread {}`
   - Be careful with thread-safe operations on shared state (`isScanning`, `deviceMap`)

4. **Data Parsing**:
   - Parsing logic is fragile - test with various BLE devices
   - Advertisement data format must follow Bluetooth Core Spec GAP data types
   - Incomplete data lines are carried over via `lineLeft` buffer
   - Hex string conversion and byte array handling is critical

5. **State Management**:
   - State management between MainActivity and BleScan needs careful coordination
   - `isScanning` flag controls scan loop in both classes
   - `isMaster` flag prevents redundant master mode calls
   - `scanJob` must be properly cancelled to avoid leaks
   - Button states are managed in `toggleScan()`

6. **Build System**:
   - Uses Gradle with Kotlin DSL (build.gradle.kts)
   - NDK support for multiple ABIs (ARM and x86 variants)
   - AAR library loaded via fileTree dependency
   - ProGuard rules may be needed for release builds

7. **Known Issues** (see TODO.md):
   - Chinese character encoding artifacts in comments
   - No resource cleanup in `onDestroy()`
   - Duplicate scanning logic (`startScan` vs `startScanAsync`)
   - No proper error handling for native library failures
   - Missing runtime permission requests

8. **Testing Considerations**:
   - Physical Android device required (BLE hardware needed)
   - Emulator testing is limited (no real BLE support)
   - Test with various BLE devices (beacons, peripherals, etc.)
   - Check Logcat for detailed scan results and errors

### File Modification Priorities

When making changes, follow this order:
1. **High Impact**: MainActivity.kt, BleScan.java
2. **Medium Impact**: AndroidManifest.xml, build.gradle.kts
3. **Low Impact**: UI resources, strings, themes
4. **Documentation**: README.md, CLAUDE.md, TODO.md

### Common Development Tasks

**Adding BLE Permissions**:
```kotlin
// Add to AndroidManifest.xml
// Implement runtime permission requests in MainActivity.onCreate()
```

**Improving Error Handling**:
```java
// In BleScan.java, check return values from At.Lib_* methods
// Add try-catch for native library exceptions
```

**Resource Cleanup**:
```kotlin
// In MainActivity.onDestroy():
override fun onDestroy() {
    super.onDestroy()
    if (isScanning) {
        isScanning = false
        scanJob?.cancel()
        bleScan.stopScan()
    }
}
```

### References

- **Main Documentation**: README.md (user-facing, includes AT API reference and flow diagrams)
- **Task Tracking**: TODO.md (roadmap, bugs, enhancements)
- **AI Context**: CLAUDE.md (this file)
- **Log Samples**: app/log/simple.txt, app/log/orginal.txt

---

## Quick Reference for AI Assistants

### Critical File Locations
- MainActivity: `app/src/main/java/com/example/test1/MainActivity.kt`
- BleScan: `app/src/main/java/com/example/test1/BleScan.java`
- Manifest: `app/src/main/AndroidManifest.xml`
- Build Config: `app/build.gradle.kts`
- Library: `app/libs/libVpos3893_release_20250930.aar`

### Key Code References
- Master mode enable: `BleScan.java:39-53`
- Start new scan: `BleScan.java:61-70`
- Continuous scan loop: `BleScan.java:141-265`
- Advertisement parsing: `BleScan.java:291-368`
- Button handlers: `MainActivity.kt:40-55`
- Toggle scan logic: `MainActivity.kt:98-121`

### Common Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean

# View build output
ls -la app/build/outputs/apk/debug/

# Check Logcat
adb logcat | grep -E "BLE_SCAN|MainActivity|TAG"
```

### Important Constants
- Buffer size: 2048 bytes
- Receive timeout: 20ms
- Max wait: 1000ms
- Max payload length: 62 hex chars (31 bytes)
- Application ID: `com.example.test1`
- Version: 1.0

### Known Bugs to Fix (from TODO.md)
1. Missing BLE permissions in AndroidManifest.xml (HIGH PRIORITY)
2. No runtime permission requests (HIGH PRIORITY)
3. `deviceMap` created inside while loop (line 151) - should be outside
4. `deviceMap` snapshot inside for loop (line 257) - should be after loop
5. No resource cleanup in `MainActivity.onDestroy()`
6. Unused `startScan()` method in MainActivity (lines 123-134)

### Testing Checklist
- [ ] Physical Android device with BLE support
- [ ] Android 7.0+ (API 24+) recommended
- [ ] Location services enabled
- [ ] BLE peripheral/beacon for testing
- [ ] adb access for Logcat monitoring
- [ ] Bluetooth enabled in device settings

### Gotchas for AI Assistants
1. **Permissions**: AndroidManifest.xml is missing ALL BLE permissions - add them first
2. **Library filename**: Changed from `libVpos3893_debug_20250307.aar` to `libVpos3893_release_20250930.aar`
3. **RSSI sign**: RSSI is negated when passed to `Lib_AtStartNewScan` (line 66 in BleScan.java)
4. **Threading**: Mixed Kotlin coroutines and Java threads - be careful with thread safety
5. **Device map scope**: `deviceMap` variable scope issue causes data loss between iterations
6. **Korean/Chinese comments**: Some comments contain non-ASCII characters causing encoding issues
7. **Unused code**: `startScan()` in MainActivity and `ComRecvAT()` in BleScan are not actively used

---

**Document Version**: 2.0
**Last Updated**: 2025-11-17
**Maintained by**: AI assistants via git commits
