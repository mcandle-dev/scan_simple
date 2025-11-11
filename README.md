# VPOS BLE App Test

This project is a minimal Android application for experimenting with Bluetooth Low Energy (BLE) scanning using a vendor provided library.

## Requirements
- **Compile SDK:** 35
- **Target SDK:** 35
- **Minimum SDK:** 24

## Building
From the repository root run:

```bash
./gradlew assembleDebug
```

The app includes the external library `libVpos3893_release_20250930.aar` under `app/libs` which contains the `At` API used for BLE communication.

## How BLE Scanning Works
- `BleScan` wraps calls to the `At` API. It enables master mode and starts a scan with `Lib_AtStartNewScan`.
- A background thread repeatedly reads data from `Lib_ComRecvAT`, parses advertisement packets and returns results via the `ScanResultListener` callback.
- `MainActivity` receives these results, logging them to `Logcat`.

### MainActivity and Layout
The main layout (`activity_main.xml`) contains three buttons:
1. **Master** – invokes `enableMasterMode`.
2. **Scan** – starts a one‑time scan via `startNewScan`.
3. **ComRev** – toggles continuous scanning and data reception.

These buttons call `Step1`, `Step2` and `toggleScan` respectively within `MainActivity`.

## AT Class API Reference

The `At` class from `vpos.apipackage` provides the core BLE functionality. Below are the main APIs:

```java
vpos.apipackage.At {
    // Master/Beacon Mode Control
    Lib_EnableMaster(boolean enable)    → Enable/disable Master mode
    Lib_EnableBeacon(boolean enable)    → Enable/disable Beacon mode

    // Beacon Parameter Management
    Lib_GetBeaconParams(Beacon beacon)  → Query Beacon settings
    Lib_SetBeaconParams(Beacon beacon)  → Update Beacon settings

    // Scanning Functions
    Lib_AtStartNewScan(String mac, String name, int rssi, String mfgId, String data)
                                        → Start BLE scan with filters
    Lib_AtStartScan(int timeout)        → Start BLE scan with timeout
    Lib_GetScanResult(int timeout, String[] devices)
                                        → Get scan results
    Lib_AtStopScan()                    → Stop ongoing scan

    // Data Reception
    Lib_ComRecvAT(byte[] data, int[] length, int timeout, int maxWait)
                                        → Receive scan data from BLE module

    // System Information
    Lib_GetAtMac(String[] mac)          → Get device MAC address
}
```

### AT Class Architecture

The `At` class serves as an **abstraction layer** between the Android application and the EFR32BG22 BLE module:

```
┌──────────────────────────────────────┐
│  Android Application Layer           │
│  (MainActivity + BleScan)            │
└───────────────┬──────────────────────┘
                │
                │ At API (libVpos3893)
                │
┌───────────────▼──────────────────────┐
│  Native Library (JNI)                │
│  (libVpos3893_release_20250930.aar)  │
└───────────────┬──────────────────────┘
                │
                │ Serial/UART Communication
                │
┌───────────────▼──────────────────────┐
│  EFR32BG22 BLE Module                │
│  (Hardware)                           │
└──────────────────────────────────────┘
```

**Key Responsibilities:**
- **Hardware Abstraction**: Hides low-level UART/serial communication details
- **Command Translation**: Converts Java method calls to AT commands
- **Data Parsing**: Receives and formats raw BLE advertisement data
- **Mode Management**: Controls Master/Beacon operating modes

## BLE Scanning Flow

### Complete Scanning Sequence

```
┌─────────────────────────────────────────────────────┐
│ 1. Initialize Master Mode                          │
│    At.Lib_EnableMaster(true)                        │
│    ↓                                                │
│    Returns: 0 (success) or error code              │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│ 2. Start BLE Scan                                   │
│    At.Lib_AtStartNewScan(                           │
│        macAddress,      // Filter by MAC (optional) │
│        broadcastName,   // Filter by name (optional)│
│        -rssi,          // RSSI threshold            │
│        manufacturerId,  // Manufacturer filter      │
│        data            // Specific data filter      │
│    )                                                │
│    ↓                                                │
│    Returns: 0 (scan started) or error code         │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│ 3. Continuous Data Reception Loop                   │
│    while (isScanning) {                             │
│        At.Lib_ComRecvAT(                            │
│            recvData,     // Buffer for received data│
│            recvDataLen,  // Length of received data │
│            20,           // Timeout (ms)            │
│            1000          // Max wait (ms)           │
│        )                                            │
│        ↓                                            │
│        Returns raw data in format:                  │
│        "MAC:AA:BB:CC:DD:EE:FF,RSSI:-50,ADV:0201..." │
│    }                                                │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│ 4. Parse Advertisement Data                         │
│    • Split by line breaks (\r\n)                    │
│    • Extract MAC, RSSI, payload type (ADV/RSP)     │
│    • Convert hex string to byte array              │
│    • Parse BLE GAP data types:                     │
│      - 0x01: Flags                                 │
│      - 0x09: Complete Local Name                   │
│      - 0xFF: Manufacturer Data                     │
│      - 0x16: Service Data (16-bit UUID)           │
│    • Build JSON object per device                  │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│ 5. Return Results via Callback                      │
│    ScanResultListener.onScanResult(                 │
│        JSONArray devices                            │
│    )                                                │
│    ↓                                                │
│    MainActivity receives and logs results          │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│ 6. Stop Scan (when needed)                          │
│    At.Lib_AtStopScan()                              │
│    isScanning = false                               │
└─────────────────────────────────────────────────────┘
```

### Step-by-Step Button Flow

**Step 1: Press "Master" Button**
```java
Step1() {
    bleScan.enableMasterMode(true)  // Initialize BLE master mode
    bleScan.getDeviceMacAddress()   // Get local device MAC
}
```

**Step 2: Press "Scan" Button**
```java
Step2() {
    bleScan.startNewScan("", "", 0, "", "")  // Start scan with no filters
}
```

**Step 3: Press "ComRev" Button**
```java
toggleScan() {
    if (!isScanning) {
        isScanning = true
        launch(Dispatchers.IO) {
            Step3()  // Start continuous scan in background
        }
    } else {
        isScanning = false
        bleScan.stopScan()  // Stop scan
    }
}

Step3() {
    bleScan.startScanAsync(sharedPreferences, scanResultListener)
    // Runs recvScanData() in loop until stopped
}
```

### Data Flow Diagram

```
User Action (Button Press)
    ↓
MainActivity.toggleScan()
    ↓
BleScan.startScanAsync()
    ↓
BleScan.recvScanData() [Background Thread]
    ↓
At.Lib_ComRecvAT() [Native Library]
    ↓
EFR32BG22 Module [Hardware]
    ↓
BLE Advertisement Packets
    ↓
Parse & Build JSON
    ↓
ScanResultListener.onScanResult()
    ↓
MainActivity logs to Logcat
```

## Permissions

The following permissions are required in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

## Documentation

- **CLAUDE.md**: Comprehensive project context for AI-assisted development
- **TODO.md**: Project roadmap and improvement tasks
