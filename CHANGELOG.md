# DARK SDK - Changelog

## [1.0.0] - 2024-01-15

### 🎉 Initial Release

#### ✨ Features
- **Professional SDK Architecture** - Clean, modular design with clear separation of concerns
- **License Management System** - Server-validated licensing with offline caching
- **Virtual Environments** - Full lifecycle management (create, start, stop, restart)
- **Command Execution** - Run shell commands in isolated environments
- **APK Installation** - Install/uninstall packages in virtual environments
- **Resource Monitoring** - Real-time stats (memory, CPU, storage, network)
- **Anti-Detection** - Built-in root hide and Xposed hide capabilities
- **Daemon Service** - Background license validation and environment management

#### 🏗️ Architecture
- **Package**: `com.dark.sdk` (professional namespace)
- **Language**: 100% Kotlin with coroutines support
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Build**: Gradle Kotlin DSL with Maven publishing ready

#### 📦 Public API
- `DarkSdk` - Main entry point (singleton)
- `DarkConfig` - Configuration data class
- `LicenseStatus` / `LicenseFeatures` / `LicenseCallback` - License management
- `EnvironmentConfig` / `ResourceLimits` - Environment configuration
- `EnvironmentHandle` - Running environment controller
- `ExecutionCallback` / `InstallationCallback` / `UninstallCallback` - Async operations

#### 🛡️ Security
- HTTPS-only communication
- ProGuard/R8 consumer rules included
- Signature-level permissions for IPC
- No sensitive data in plain text

#### 📚 Documentation
- Comprehensive README with examples
- Inline KDoc documentation
- Sample application demonstrating all features

---

## [Unreleased]

### Planned
- [ ] WebSocket support for real-time license updates
- [ ] Environment snapshots/cloning
- [ ] Network traffic interception
- [ ] Custom kernel module support
- [ ] Multi-device license sync
- [ ] Gradle plugin for easy integration