# How to Build the Neighborhood Helper App

## Option 1: Using Android Studio (RECOMMENDED - EASIEST)

### Steps:
1. **Open Android Studio**
2. **Open the project**: File → Open → Select `d:\neighborhood-helper`
3. **Wait for Gradle sync**: Android Studio will automatically sync Gradle files (may take 2-3 minutes)
4. **Build the app**: 
   - Click **Build** menu → **Make Project**
   - Or press **Ctrl+F9**
5. **Run the app**: 
   - Click the green **Run** button (or press **Shift+F10**)
   - Select an emulator or connected device
6. **App launches** with Create Post screen

---

## Option 2: Using Command Line (REQUIRES Java & Internet)

### Prerequisites:
1. **Java 11+** installed and in PATH
   ```
   java -version
   ```
   Should show version 11 or higher

2. **Internet connection** (to download Gradle 8.13 and dependencies)

### Steps:
```powershell
# Navigate to project
cd d:\neighborhood-helper

# Download Gradle wrapper and dependencies (happens automatically)
.\gradlew.bat assembleDebug

# When complete, APK will be at:
# app/build/outputs/apk/debug/app-debug.apk

# Install on device:
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Option 3: If Gradle Downloads Fail

### Manual Gradle Installation:
1. Download Gradle 8.13 from: https://gradle.org/releases/
2. Extract to: `C:\gradle-8.13`
3. Set environment variable:
   - **GRADLE_HOME** = `C:\gradle-8.13`
   - Add to **PATH**: `C:\gradle-8.13\bin`
4. Run:
   ```
   gradle assembleDebug
   ```

---

## What Happens During Build

1. **Gradle downloads** (first time only) - ~200MB
2. **Dependencies resolve** - AndroidX, Compose, Firebase
3. **Kotlin code compiles** - All .kt files checked
4. **Resources processed** - Strings, drawables, etc.
5. **APK assembled** - Binary app package created
6. **Result**: `app/build/outputs/apk/debug/app-debug.apk`

**Total time**: 2-5 minutes (first build), 30-60 seconds (subsequent builds)

---

## Verify Installation

After successful build, check:
```
# Should exist and be ~50-100MB
app/build/outputs/apk/debug/app-debug.apk

# List files:
dir app\build\outputs\apk\debug\
```

---

## If You Get Errors

### "Could not find or load main class"
- Gradle wrapper JAR will auto-download on first run
- Ensure internet connection is active
- Wait 2-3 minutes for first run

### "JAVA_HOME is not set"
```powershell
# Check Java installation
java -version

# Set JAVA_HOME (if needed)
$env:JAVA_HOME="C:\Program Files\Java\jdk-11"
```

### "Permission denied"
```powershell
# On Windows, run PowerShell as Administrator
# Then retry the build command
```

---

## SUCCESS Indicators

✅ **Build successful** when you see:
```
BUILD SUCCESSFUL in X seconds
```

❌ **Build failed** if you see:
```
BUILD FAILED in X seconds
```

---

## Quick Reference

| Command | Purpose |
|---------|---------|
| `.\gradlew.bat clean` | Clean build cache |
| `.\gradlew.bat assembleDebug` | Build APK |
| `.\gradlew.bat build` | Full build + tests |
| `adb install app/build/outputs/apk/debug/app-debug.apk` | Install on device |
| `adb uninstall com.example.neighborhoodhelper` | Uninstall app |

---

## Next: Run the App in Android Studio

Once build succeeds:
1. Click **Run** button (green play icon)
2. Select emulator or device
3. App launches with **Create Post Screen**
4. Test the UI flow

Enjoy! 🎉

