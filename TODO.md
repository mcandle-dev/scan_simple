# TODO List - VPOS BLE App Test

## High Priority

### Code Quality
- [ ] Clean up Chinese character encoding issues in comments (`BleScan.java:157, 164, 182, etc.`)
- [ ] Add proper error handling in `recvScanData()` loop
- [ ] Add resource cleanup in `MainActivity.onDestroy()` (stop scanning, cancel jobs)
- [ ] Remove unused `startScan()` method in `MainActivity.kt:123-134`
- [ ] Consolidate duplicate scanning logic in `BleScan` (`startScan` vs `startScanAsync`)

### Bug Fixes
- [ ] Fix potential memory leak: `scanJob` is not properly cancelled in all scenarios
- [ ] Handle case where `Lib_ComRecvAT` returns error codes
- [ ] Fix synchronization issue in `recvScanData()`: `deviceMap` snapshot created inside loop
- [ ] Validate SharedPreferences input before passing to native library
- [ ] Handle case where BLE permissions are denied at runtime

### State Management
- [ ] Add proper state validation (e.g., can't scan without master mode enabled)
- [ ] Prevent multiple simultaneous scan operations
- [ ] Add timeout mechanism for hanging scans
- [ ] Persist last known scan state across app restarts

## Medium Priority

### UI/UX Improvements
- [ ] Display scan results in RecyclerView instead of just logging
- [ ] Add device count indicator
- [ ] Show current scanning status (idle/scanning)
- [ ] Add visual feedback for button actions (loading indicators)
- [ ] Display parsed BLE data in readable format (not just raw JSON)
- [ ] Add filter UI for MAC address, RSSI threshold, device name

### Features
- [ ] Implement device detail view (show all advertisement data)
- [ ] Add ability to export scan results to CSV/JSON file
- [ ] Add scan duration timer
- [ ] Implement RSSI-based sorting/filtering
- [ ] Add device caching to avoid duplicate processing
- [ ] Support continuous scan with auto-refresh

### Testing
- [ ] Add unit tests for `parseAdvertisementData()`
- [ ] Add integration tests for BLE scanning flow
- [ ] Test with various BLE devices (beacons, fitness trackers, etc.)
- [ ] Test permission denial scenarios
- [ ] Test background/foreground transitions during scanning

## Low Priority

### Optimization
- [ ] Reduce log verbosity (too many debug logs in production)
- [ ] Optimize JSON parsing performance
- [ ] Use more efficient data structures for device tracking
- [ ] Reduce main thread work (move more to background)
- [ ] Implement pagination for large device lists

### Documentation
- [ ] Add KDoc/JavaDoc comments to public methods
- [ ] Document `libVpos3893_debug_20250307.aar` API limitations
- [ ] Create architecture diagram
- [ ] Add code examples for common use cases
- [ ] Document known BLE advertisement data formats

### Refactoring
- [ ] Convert `BleScan.java` to Kotlin for consistency
- [ ] Use sealed classes for scan states
- [ ] Implement proper dependency injection (Hilt/Koin)
- [ ] Separate UI logic from business logic (MVVM/MVI pattern)
- [ ] Extract BLE parsing logic to separate module

### Infrastructure
- [ ] Set up CI/CD pipeline (GitHub Actions)
- [ ] Add ProGuard rules for release builds
- [ ] Configure code linting (ktlint, detekt)
- [ ] Add crash reporting (Firebase Crashlytics)
- [ ] Set up automated testing

## Future Enhancements

### Advanced Features
- [ ] Support BLE connection (not just scanning)
- [ ] Implement GATT service discovery
- [ ] Add ability to write to BLE characteristics
- [ ] Support multiple simultaneous BLE connections
- [ ] Add BLE device pairing/bonding

### Analytics
- [ ] Track most common device types scanned
- [ ] Log scan success/failure rates
- [ ] Monitor battery usage during scanning
- [ ] Track user engagement metrics

### Multi-device Support
- [ ] Test on various Android devices (Samsung, Pixel, etc.)
- [ ] Handle device-specific BLE quirks
- [ ] Optimize for tablets and foldables

## Technical Debt

### Library Management
- [ ] Update external library to latest version
- [ ] Document library version compatibility
- [ ] Create abstraction layer over `At` API to reduce coupling
- [ ] Consider replacing proprietary library with open-source alternative

### Security
- [ ] Audit library for security vulnerabilities
- [ ] Add ProGuard obfuscation for release builds
- [ ] Validate all input from native library
- [ ] Add permission request flow with rationale

### Compatibility
- [ ] Test on Android 14+ (Target SDK 35)
- [ ] Handle deprecated APIs (if any)
- [ ] Test on devices with/without BLE support

## Completed
- [x] Initial project setup
- [x] Basic BLE scanning implementation
- [x] UI with 3-button control
- [x] Advertisement data parsing
- [x] JSON result formatting
- [x] README documentation
- [x] CLAUDE.md context documentation

## Notes
- Current git status shows modified files: `.idea/misc.xml`, `app/src/main/res/values/strings.xml`
- Deleted file: `app/libs/libVpos3893_debug_20250307.aar` (but still referenced in code)
- Untracked files in `app/libs/` directory - need to check what's there

## Before Next Release
- [ ] Resolve git status issues (commit or revert changes)
- [ ] Verify library file exists in `app/libs/`
- [ ] Update version number in `build.gradle`
- [ ] Test on physical device with real BLE peripherals
- [ ] Create release notes

---
Last Updated: 2025-11-12
