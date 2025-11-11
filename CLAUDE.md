# CLAUDE.md - Project Context for AI Assistance

## Project Overview
**VPOS BLE App Test** is a minimal Android application designed for experimenting with Bluetooth Low Energy (BLE) scanning using a vendor-provided library (`libVpos3893_debug_20250307.aar`). The app demonstrates how to scan for BLE devices, parse advertisement data, and display results.

## Tech Stack
- **Language**: Kotlin (MainActivity) + Java (BleScan)
- **Platform**: Android
- **Compile SDK**: 35
- **Target SDK**: 35
- **Minimum SDK**: 24
- **Build System**: Gradle
- **External Library**: `libVpos3893_debug_20250307.aar` (VPOS BLE library)
- **Concurrency**: Kotlin Coroutines + Java Threads

## Project Structure

```
mcandle-simple/
├── app/
│   ├── libs/
│   │   └── libVpos3893_debug_20250307.aar  # External VPOS BLE library
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/test1/
│   │       │   ├── MainActivity.kt          # Main activity with UI controls
│   │       │   └── BleScan.java            # BLE scanning logic wrapper
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml   # UI layout with 3 buttons
│   │       │   └── values/
│   │       │       └── strings.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── README.md
└── CLAUDE.md (this file)
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
- `Step1()`: Enables master mode using `bleScan.enableMasterMode(true)`
- `Step2()`: Starts new scan with `bleScan.startNewScan()`
- `Step3()`: Starts async scan with result listener
- `toggleScan()`: Manages continuous scanning state

### 2. BleScan.java (`app/src/main/java/com/example/test1/BleScan.java`)
Wrapper class for the VPOS BLE library's `At` API.

**Key Features**:
- Wraps native `At` library calls
- Parses BLE advertisement data (ADV/RSP packets)
- Provides callback interfaces for data reception
- Runs scanning in background threads

**Core Methods**:
- `enableMasterMode(boolean)`: Enables/disables BLE master mode
- `startNewScan()`: Initiates a new BLE scan
- `startScanAsync()`: Starts async scanning with SharedPreferences filters
- `stopScan()`: Stops ongoing scan
- `recvScanData()`: Continuously receives and parses scan data
- `parseAdvertisementData()`: Parses BLE advertisement packets into JSON

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

### 3. VPOS Library (`libVpos3893_debug_20250307.aar`)
External vendor library providing the `At` API for BLE operations.

**Key API Methods**:
- `At.Lib_EnableMaster(boolean)`: Enable/disable master mode
- `At.Lib_GetAtMac(String[])`: Get device MAC address
- `At.Lib_AtStartNewScan()`: Start BLE scan with filters
- `At.Lib_ComRecvAT()`: Receive scan data
- `At.Lib_AtStopScan()`: Stop scanning

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
- Data is received as byte array from `Lib_ComRecvAT()`
- Lines are split by `\r\n|\r|\n`
- Last incomplete line is carried over to next iteration (`lineLeft`)
- Only lines starting with "MAC:" are processed
- Advertisement data is parsed according to Bluetooth Core Spec (GAP data types)

### Known Issues
- Some Chinese comments in code (encoding artifacts like `����` visible)
- `startScan()` method duplicates logic with `startScanAsync()`
- Error handling could be improved (many `continue` statements in loops)
- No proper resource cleanup in MainActivity `onDestroy()`

## Permissions Required
In `AndroidManifest.xml`:
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
- `ACCESS_FINE_LOCATION` (required for BLE on Android)

## Debug Tips
- Check Logcat with filter: `TAG` or `BLE_SCAN` or `MainActivity`
- Common log messages:
  - `"Lib_ComRecvAT buff:"` - Raw received data
  - `"Received Scan Data:"` - Parsed JSON results
  - `"Master mode updated successfully"` - Master mode status
  - `"BLE Scan Started with result:"` - Scan initialization

## Development Context
This is version 1.0 of the project. Recent commits show:
- Initial 1.0 release
- README updates with build and BLE info
- Recent library update (removed old `libVpos3893_debug_20250307.aar`)

## Working with This Project

When modifying this project, keep in mind:
1. The VPOS library is proprietary - its behavior is a black box
2. BLE scanning requires proper permission handling on Android
3. Threading is mixed (Kotlin coroutines + Java threads)
4. Parsing logic is fragile - test with various BLE devices
5. State management between MainActivity and BleScan needs careful coordination
